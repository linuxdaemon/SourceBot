/*
 * Copyright (c) 2016.
 * This file is part of SourceBot.
 *
 * SourceBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SourceBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SourceBot.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.walterbarnes.sourcebot;

import com.google.gson.JsonObject;
import net.walterbarnes.sourcebot.command.CommandHandler;
import net.walterbarnes.sourcebot.config.Config;
import net.walterbarnes.sourcebot.crash.CrashReport;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;
import net.walterbarnes.sourcebot.thread.InputThread;
import net.walterbarnes.sourcebot.tumblr.Tumblr;
import net.walterbarnes.sourcebot.util.LogHelper;
import org.scribe.exceptions.OAuthConnectionException;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SourceBot
{
	private static final String CLASS_NAME = SourceBot.class.getName();
	private static SourceBot currentBot;
	private final String confName = "SourceBot.json";
	private final Logger logger = Logger.getLogger(SourceBot.class.getName());
	public volatile boolean running = true;
	public Thread currentThread;
	public InputThread inputThread = new InputThread();
	public Tumblr client;
	public Map<String, SearchThread> threads = new HashMap<>();
	File confDir = new File(System.getProperty("user.home"), ".sourcebot");
	private Config conf;
	private Connection conn;
	private CommandHandler commandHandler;

	public static void main(String[] args)
	{
		SourceBot sb = getCurrentBot();
		;
		try
		{
			LogHelper.init();
			Thread t = new Thread(sb.inputThread, "Console Input Handler");
			t.setDaemon(true);
			t.start();

			if (args.length > 0)
			{
				sb.confDir = new File(args[0]);
				sb.logger.info(String.format("Set config dir to %s", sb.confDir.getAbsolutePath()));
			}

			if (!sb.confDir.exists())
			{
				if (!sb.confDir.mkdirs())
				{
					throw new RuntimeException("Unable to create config dir");
				}
			}

			File jsonFile = new File(sb.confDir, sb.confName);

			if (Arrays.asList(args).contains("install") || !jsonFile.exists())
			{
				Install.install(sb.confDir.getAbsolutePath(), sb.confName);
				System.exit(0);
			}

			sb.conf = new Config(sb.confDir.getAbsolutePath(), sb.confName);
			sb.getCommandHandler();
			sb.run();
		}
		catch (Throwable throwable)
		{
			displayCrashReport(new CrashReport("Unexpected error", throwable));
		}
		finally
		{
			if (sb.currentThread != null)
			{
				sb.currentThread.interrupt();
			}
			if (sb.conn != null)
			{
				try
				{
					sb.conn.close();
				}
				catch (SQLException e)
				{
					sb.logger.warning("Error occurred on closing connection to database");
				}
			}
		}
	}

	public static SourceBot getCurrentBot()
	{
		if (currentBot == null)
		{ currentBot = new SourceBot(); }
		return currentBot;
	}

	public CommandHandler getCommandHandler()
	{
		if (commandHandler == null)
		{ commandHandler = new CommandHandler(); }
		return commandHandler;
	}

	private void run() throws InvalidBlogNameException, SQLException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException
	{
		Driver driver = (Driver) Class.forName("org.postgresql.Driver").newInstance();

		Config apiCat = conf.getCategory("api", new JsonObject());
		Config consumerCat = apiCat.getCategory("consumer", new JsonObject());
		Config tokenCat = apiCat.getCategory("token", new JsonObject());

		Config dbCat = conf.getCategory("db", new JsonObject());
		String consumerKey = consumerCat.getString("key", "");
		String consumerSecret = consumerCat.getString("secret", "");
		String token = tokenCat.getString("key", "");
		String tokenSecret = tokenCat.getString("secret", "");

		this.client = new Tumblr(consumerKey, consumerSecret, token, tokenSecret);

		String dbHost = dbCat.getString("host", "");
		String dbPort = dbCat.getString("port", "");
		String dbUser = dbCat.getString("user", "");
		String dbPass = dbCat.getString("pass", "");
		String dbName = dbCat.getString("db_name", "");
		conf.save();

		final Connection conn = DriverManager.getConnection(String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName),
				dbUser, dbPass);

		Thread botThread = new Thread()
		{
			public void run()
			{
				try
				{
					PreparedStatement getBlogs = conn.prepareStatement("SELECT url,active,adm_active FROM blogs ORDER BY id;",
							ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

					ResultSet rs = getBlogs.executeQuery();

					long queryTime = System.currentTimeMillis();

					while (running)
					{
						try
						{
							if ((System.currentTimeMillis() - queryTime) > 60000)
							{
								rs = getBlogs.executeQuery();
								queryTime = System.currentTimeMillis();
							}

							rs.beforeFirst();
							while (rs.next() && running)
							{
								String url = rs.getString("url");
								boolean active = rs.getBoolean("active");
								boolean adm_active = rs.getBoolean("adm_active");
								if (active && adm_active)
								{
									if (!threads.containsKey(url))
									{
										SearchThread bt = new SearchThread(client, url, conn);
										threads.put(url, bt);
									}
									logger.info("Running Thread for " + url);
									long start = System.currentTimeMillis();
									currentThread = new Thread(threads.get(url));
									currentThread.start();
									currentThread.join();
									logger.info("Took " + (System.currentTimeMillis() - start) + " ms");
								}
							}
							Thread.sleep(1000);
						}
						catch (OAuthConnectionException e)
						{
							logger.log(Level.SEVERE, e.getMessage(), e);
						}
						catch (InterruptedException ignored)
						{
							Thread.currentThread().interrupt();
						}
					}
				}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		};
		//botThread.setDaemon(true);
		botThread.start();
	}

	/**
	 * Displays a crash report and saves it to a file
	 *
	 * @param crashReport Report to display
	 */
	private static void displayCrashReport(CrashReport crashReport)
	{
		File file1 = new File(".", "crash-reports");
		File file2 = new File(file1, "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss"))
				.format(new Date()) + ".txt");
		System.out.println(crashReport.getCompleteReport());

		if (crashReport.getFile() != null)
		{
			System.out.println("#@!@# Bot crashed! Crash report saved to: #@!@# " + crashReport.getFile());
		}
		else if (crashReport.saveToFile(file2))
		{
			System.out.println("#@!@# Bot crashed! Crash report saved to: #@!@# " + file2.getAbsolutePath());
		}
		else
		{
			System.out.println("#@?@# Bot crashed! Crash report could not be saved. #@?@#");
		}
	}
}
