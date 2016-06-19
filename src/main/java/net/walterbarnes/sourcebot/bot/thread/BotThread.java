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

package net.walterbarnes.sourcebot.bot.thread;

import net.walterbarnes.sourcebot.bot.SourceBot;
import net.walterbarnes.sourcebot.bot.config.DB;
import net.walterbarnes.sourcebot.bot.config.types.BlogConfig;
import net.walterbarnes.sourcebot.bot.crash.CrashReport;
import net.walterbarnes.sourcebot.bot.tumblr.Tumblr;
import net.walterbarnes.sourcebot.bot.util.LogHelper;

import java.util.List;

public class BotThread extends Thread
{
	private final Tumblr client;
	private final String dbHost;
	private final String dbPort;
	private final String dbName;
	private final String dbUser;
	private final String dbPass;
	private boolean simulate;
	private volatile boolean running = true;

	public BotThread(Tumblr client, String dbHost, String dbPort, String dbName, String dbUser, String dbPass, boolean simulate)
	{
		super("BotThread");
		this.client = client;
		this.dbHost = dbHost;
		this.dbPort = dbPort;
		this.dbName = dbName;
		this.dbUser = dbUser;
		this.dbPass = dbPass;
		this.simulate = simulate;
	}

	@Override
	public void run()
	{
		LogHelper.info("thread started");
		try (DB db = new DB(client, dbHost, Integer.parseInt(dbPort), dbName, dbUser, dbPass))
		{
			db.setDriver("org.postgresql.Driver").setScheme("jdbc:postgresql").connect();
			SourceBot sb = SourceBot.INSTANCE;
			while (running)
			{
				try
				{
					LogHelper.info("getting blogs");
					List<BlogConfig> blogs = db.getAllBlogs();
					LogHelper.info("done");
					LogHelper.info("getting # of active blogs");
					sb.botStatus.setActiveBlogs((int) blogs.stream().filter(BlogConfig::isActive).count());
					LogHelper.info("done");
					for (BlogConfig blog : blogs)
					{
						if (!running)
							break;
						String url = blog.getUrl();
						if (!sb.threads.containsKey(url))
						{
							SearchThread st = new SearchThread(client, blog);
							sb.threads.put(url, st);
						}
						if (blog.isActive() && blog.isAdmActive())
						{
							LogHelper.info("Running thread for " + url);
							long start = System.currentTimeMillis();
							sb.currentThread = new Thread(sb.threads.get(url).setSimulate(simulate));
							sb.currentThread.setName("SearchThread");
							sb.currentThread.start();
							sb.currentThread.join();
							LogHelper.debug(String.format("Took %d ms", System.currentTimeMillis() - start));
						}
					}
				}
				catch (InterruptedException ignored)
				{
					Thread.currentThread().interrupt();
					running = false;
				}
			}
		}
		catch (Throwable throwable)
		{
			CrashReport.displayCrashReport(new CrashReport("Error in search thread", throwable));
		}
	}
}