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

package net.walterbarnes.sourcebot.bot;

import com.google.gson.JsonObject;
import net.walterbarnes.sourcebot.bot.command.CommandHandler;
import net.walterbarnes.sourcebot.bot.thread.InputThread;
import net.walterbarnes.sourcebot.common.cli.Cli;
import net.walterbarnes.sourcebot.common.config.Configuration;
import net.walterbarnes.sourcebot.common.config.DB;
import net.walterbarnes.sourcebot.common.config.types.BlogConfig;
import net.walterbarnes.sourcebot.common.crash.CrashReport;
import net.walterbarnes.sourcebot.common.reference.Constants;
import net.walterbarnes.sourcebot.common.tumblr.Tumblr;
import net.walterbarnes.sourcebot.common.util.LogHelper;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SourceBot
{
	/**
	 * Static link to current SourceBot instance
	 */
	private static final SourceBot currentBot = new SourceBot();

	/**
	 * Default config file name, in the future, this may be overridden via command line arguments
	 */
	public final String confName = "SourceBot.json";
	public final Map<String, SearchThread> threads = new HashMap<>();
	private final Logger logger = Logger.getLogger(SourceBot.class.getName());
	private final InputThread inputThread = new InputThread();
	public volatile boolean running = true;
	public Thread currentThread;
	public Tumblr client;
	/**
	 * Default configuration directory, can be overridden via command-line arguments
	 */
	public File confDir = new File(System.getProperty("user.home"), ".sourcebot");

	private Configuration conf;
	private CommandHandler commandHandler;

	public static void main(String[] args)
	{
		try
		{
			// Initialize and configure the root logger
			LogHelper.init();

			// Start new user command handler
			currentBot.commandHandler = new CommandHandler();

			Thread t = new Thread(currentBot.inputThread, "Console Input Handler");
			t.setDaemon(true);
			t.start();

			// Parse and handle command line arguments
			new Cli(args).parse();

			// Check existence of config directory
			if (!currentBot.confDir.exists())
			{
				// Attempt to create the directory
				if (!currentBot.confDir.mkdirs())
				{
					throw new RuntimeException("Unable to create config dir");
				}
			}

			// Create config instance
			currentBot.conf = new Configuration(currentBot.confDir.getAbsolutePath(), currentBot.confName);

			// If the config file doesn't exists, run the install process
			if (!currentBot.conf.exists())
			{
				Install.install(currentBot.confDir.getAbsolutePath(), currentBot.confName);
				System.exit(0);
			}

			// Load/read config
			currentBot.conf.init();

			Constants.load(currentBot.conf);

			//ConfigServer cs = new ConfigServer(8087);
			//cs.addPage("/connect", new ConnectHandler(Constants.getConsumerKey(), Constants.getConsumerSecret()));
			//cs.addPage("/callback", new CallbackHandler());
			//cs.addPage("/rules", new RuleManagerHandler());
			//cs.addPage("/blogs", new BlogManagerHandler());
			//cs.addPage("/", new IndexHandler());
			//cs.start();

			// Run main thread
			currentBot.run();
		}
		catch (Throwable throwable)
		{
			CrashReport.displayCrashReport(new CrashReport("Unexpected error", throwable));
		}
		finally
		{
			// Shutdown sequence
			if (currentBot.currentThread != null)
			{
				currentBot.currentThread.interrupt();
			}
		}
	}

	private void run()
	{
		this.client = new Tumblr(Constants.getConsumerKey(), Constants.getConsumerSecret(), Constants.getToken(), Constants.getTokenSecret());

		Configuration dbCat = conf.getCategory("db", new JsonObject());

		String dbHost = dbCat.getString("host", "");
		String dbPort = dbCat.getString("port", "");
		String dbUser = dbCat.getString("user", "");
		String dbPass = dbCat.getString("pass", "");
		String dbName = dbCat.getString("dbName", "");
		if (conf.hasChanged()) conf.save();

		try (DB db = new DB(client, dbHost, Integer.parseInt(dbPort), dbName, dbUser, dbPass))
		{
			db.setDriver("org.postgresql.Driver");
			db.setScheme("jdbc:postgresql");

			Thread botThread = new Thread()
			{
				public void run()
				{
					try
					{
						while (running)
						{
							try
							{
								for (BlogConfig blog : db.getAllBlogs())
								{
									if (!running)
										break;
									String url = blog.getUrl();
									if (!threads.containsKey(url))
									{
										SearchThread st = new SearchThread(client, blog);
										threads.put(url, st);
									}
									if (blog.isActive() && blog.isAdmActive())
									{
										logger.info("Running thread for " + url);
										long start = System.currentTimeMillis();
										currentThread = new Thread(threads.get(url));
										currentThread.start();
										currentThread.join();
										logger.fine(String.format("Took %d ms", System.currentTimeMillis() - start));
									}
								}
							}
							catch (InterruptedException ignored)
							{
								Thread.currentThread().interrupt();
							}
						}
					}
					catch (Throwable throwable)
					{
						CrashReport.displayCrashReport(new CrashReport("Error in search thread", throwable));
					}
				}
			};
			botThread.start();
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw new RuntimeException("Database Error Occurred, exiting...");
		}
	}

	/**
	 * Gets the current instance of the bot, or creates one if none is set
	 *
	 * @return the bot instance
	 */
	public static SourceBot getCurrentBot()
	{
		return currentBot;
	}

	/**
	 * Gets the current bots CommandHandler instance, or creates one if none is set
	 *
	 * @return the CommandHandler instance
	 */
	public CommandHandler getCommandHandler()
	{
		return commandHandler;
	}
}
