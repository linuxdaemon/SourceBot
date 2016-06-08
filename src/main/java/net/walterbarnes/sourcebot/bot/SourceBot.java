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

import net.walterbarnes.sourcebot.bot.cli.Cli;
import net.walterbarnes.sourcebot.bot.command.CommandHandler;
import net.walterbarnes.sourcebot.bot.config.Configuration;
import net.walterbarnes.sourcebot.bot.crash.CrashReport;
import net.walterbarnes.sourcebot.bot.reference.Constants;
import net.walterbarnes.sourcebot.bot.thread.BotThread;
import net.walterbarnes.sourcebot.bot.thread.InputThread;
import net.walterbarnes.sourcebot.bot.thread.SearchThread;
import net.walterbarnes.sourcebot.bot.tumblr.Tumblr;
import net.walterbarnes.sourcebot.bot.util.LogHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

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
	public final BotStatus botStatus = new BotStatus();
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
			INSTANCE.botStatus.setStage("Starting up");
			INSTANCE.writePidToFile();
			// Initialize and configure the root logger
			LogHelper.init();

			// Start new user command handler
			INSTANCE.commandHandler = new CommandHandler();

			Thread t = new Thread(INSTANCE.inputThread, "Console Input Handler");
			t.setDaemon(true);
			t.start();

			// Parse and handle command line arguments
			new Cli(args).parse();

			INSTANCE.botStatus.setStage("Loading configs");
			// Create config instance
			INSTANCE.conf = new Configuration(INSTANCE.confDir.getAbsolutePath(), INSTANCE.confName);

			// Run main thread
			INSTANCE.run(Constants.simulate);
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

	private void writePidToFile() throws IOException
	{
		String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		File f = new File(confDir, "bot.pid");
		f.createNewFile();
		FileWriter fw = new FileWriter(f);
		fw.write(pid);
		fw.close();
	}

	private void run(boolean simulate) throws IOException
	{
		botStatus.setStage("Running search thread" + (simulate ? " (simulation)" : ""));
		this.client = new Tumblr(conf.consumerKey, conf.consumerSecret, conf.token, conf.tokenSecret);

		botStatus.setSimulate(simulate);
		if (simulate) LogHelper.info("Simulating search");
		LogHelper.info("creating thread");
		BotThread bt = new BotThread(client, conf.dbHost, conf.dbPort, conf.dbName, conf.dbUser, conf.dbPass, simulate);
		LogHelper.info("created");
		LogHelper.info("starting thread");
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
