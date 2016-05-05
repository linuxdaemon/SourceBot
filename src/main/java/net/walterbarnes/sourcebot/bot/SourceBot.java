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
import net.walterbarnes.sourcebot.bot.thread.BotThread;
import net.walterbarnes.sourcebot.bot.thread.InputThread;
import net.walterbarnes.sourcebot.bot.thread.SearchThread;
import net.walterbarnes.sourcebot.common.cli.Cli;
import net.walterbarnes.sourcebot.common.command.CommandHandler;
import net.walterbarnes.sourcebot.common.config.Configuration;
import net.walterbarnes.sourcebot.common.crash.CrashReport;
import net.walterbarnes.sourcebot.common.reference.Constants;
import net.walterbarnes.sourcebot.common.tumblr.Tumblr;
import net.walterbarnes.sourcebot.common.util.LogHelper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class SourceBot
{
	/**
	 * Static link to current SourceBot instance
	 */
	public static final SourceBot INSTANCE = new SourceBot();

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
			INSTANCE.commandHandler = new CommandHandler();

			Thread t = new Thread(INSTANCE.inputThread, "Console Input Handler");
			t.setDaemon(true);
			t.start();

			// Parse and handle command line arguments
			new Cli(args).parse();

			// Check existence of config directory
			if (!INSTANCE.confDir.exists())
			{
				// Attempt to create the directory
				if (!INSTANCE.confDir.mkdirs())
				{
					throw new RuntimeException("Unable to create config dir");
				}
			}

			// Create config instance
			INSTANCE.conf = new Configuration(INSTANCE.confDir.getAbsolutePath(), INSTANCE.confName);

			// If the config file doesn't exist, run the install process
			if (!INSTANCE.conf.exists())
			{
				Install.install(INSTANCE.confDir.getAbsolutePath(), INSTANCE.confName);
				System.exit(0);
			}

			// Load/read config
			INSTANCE.conf.init();

			Constants.load(INSTANCE.conf);

			//ConfigServer cs = new ConfigServer(8087);
			//cs.addPage("/connect", new ConnectHandler(Constants.getConsumerKey(), Constants.getConsumerSecret()));
			//cs.addPage("/callback", new CallbackHandler());
			//cs.addPage("/rules", new RuleManagerHandler());
			//cs.addPage("/blogs", new BlogManagerHandler());
			//cs.addPage("/", new IndexHandler());
			//cs.start();

			// Run main thread
			INSTANCE.run();
		}
		catch (Throwable throwable)
		{
			CrashReport.displayCrashReport(new CrashReport("Unexpected error", throwable));
		}
		finally
		{
			// Shutdown sequence
			if (INSTANCE.currentThread != null)
			{
				INSTANCE.currentThread.interrupt();
			}
		}
	}

	private void run()
	{
		this.client = new Tumblr(Constants.getConsumerKey(), Constants.getConsumerSecret(), Constants.getToken(), Constants.getTokenSecret());

		Configuration dbCat = conf.getCategory("db", new JsonObject());

		final String dbHost = dbCat.getString("host", "");
		final String dbPort = dbCat.getString("port", "");
		final String dbUser = dbCat.getString("user", "");
		final String dbPass = dbCat.getString("pass", "");
		final String dbName = dbCat.getString("dbName", "");
		if (conf.hasChanged()) conf.save();

		BotThread bt = new BotThread(client, dbHost, dbPort, dbName, dbUser, dbPass);
		bt.start();
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
