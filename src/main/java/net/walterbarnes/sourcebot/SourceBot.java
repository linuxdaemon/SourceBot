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
	private static final String confName = "SourceBot.json";
	private static final Logger logger = Logger.getLogger(SourceBot.class.getName());
	public static volatile boolean running = true;
	public static Thread currentThread;
	public static InputThread inputThread = new InputThread();
	static File confDir = new File(System.getProperty("user.home"), ".sourcebot");
	private static Config conf;
	private static Connection conn;

	public static void main(String[] args)
	{
		Thread t = null;
		try
		{
			LogHelper.init();
			t = new Thread(inputThread);
			t.start();

			if (args.length > 0)
			{
				confDir = new File(args[0]);
				logger.info(String.format("Set config dir to %s", confDir.getAbsolutePath()));
			}

			if (!confDir.exists())
			{
				if (!confDir.mkdirs())
				{
					throw new RuntimeException("Unable to create config dir");
				}
			}

			File jsonFile = new File(confDir, confName);

			if (Arrays.asList(args).contains("install") || !jsonFile.exists())
			{
				Install.install(confDir.getAbsolutePath(), confName);
				System.exit(0);
			}

			conf = new Config(confDir.getAbsolutePath(), confName);
			run();
		}
		catch (Throwable throwable)
		{
			displayCrashReport(new CrashReport("Unexpected error", throwable));
		}
		finally
		{
			if (t != null)
			{
				t.interrupt();
			}
			if (currentThread != null)
			{
				currentThread.interrupt();
			}
			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException e)
				{
					logger.warning("Error occurred on closing connection to database");
				}
			}
		}
	}

	private static void run() throws InvalidBlogNameException, SQLException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException
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

		Tumblr client = new Tumblr(consumerKey, consumerSecret, token, tokenSecret);

		String dbHost = dbCat.getString("host", "localhost");
		String dbPort = dbCat.getString("port", "5432");
		String dbUser = dbCat.getString("user", "sourcebot");
		String dbPass = dbCat.getString("pass", "password");
		String dbName = dbCat.getString("db_name", "sourcebot");
		conf.save();

		conn = DriverManager.getConnection(String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName),
				dbUser, dbPass);
		PreparedStatement getBlogs = conn.prepareStatement("SELECT url,active,adm_active FROM blogs ORDER BY id;",
				ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet rs = getBlogs.executeQuery();

		long queryTime = System.currentTimeMillis();

		Map<String, BotThread> threads = new HashMap<>();

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
							BotThread bt = new BotThread(client, url, conn);
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
			}
		}
	}

	/**
	 * Displays a crash report and saves it to a file
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
