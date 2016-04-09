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
import net.walterbarnes.sourcebot.reference.Constants;
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

public class SourceBot
{
	static final String confName = "SourceBot.json";
	static File confDir = new File(System.getProperty("user.home"), ".sourcebot");
	private static String[] args;
	private static Config conf;

	public static void main(String[] args)
	{
		SourceBot.args = args;
		CrashReport crashreport;
		try
		{
			if (args.length > 0)
			{
				confDir = new File(args[0]);
			}
			LogHelper.init(SourceBot.class);

			if (!confDir.exists())
			{ confDir.mkdirs(); }
			File jsonFile = new File(confDir, confName);
			Constants.LOGGER.info(jsonFile.getAbsolutePath());

			if (Arrays.asList(args).contains("install") || !jsonFile.exists())
			{
				Install.install();
				System.exit(0);
			}
			conf = new Config(confDir.getAbsolutePath(), confName);
			Class.forName("org.postgresql.Driver").newInstance();
			run();
		}
		catch (Throwable throwable)
		{
			crashreport = new CrashReport("Unexpected error", throwable);
			displayCrashReport(crashreport);
		}
	}

	private static void run() throws InvalidBlogNameException, SQLException, IOException,
			InstantiationException, IllegalAccessException
	{
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

		Connection conn = DriverManager.getConnection(String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName),
				dbUser, dbPass);
		PreparedStatement getBlogs = conn.prepareStatement("SELECT url,active,adm_active FROM blogs ORDER BY id;",
				ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet rs = getBlogs.executeQuery();

		long queryTime = System.currentTimeMillis();

		Map<String, BotThread> threads = new HashMap<>();

		//noinspection InfiniteLoopStatement
		while (true)
		{
			try
			{
				if ((System.currentTimeMillis() - queryTime) > 60000)
				{
					rs = getBlogs.executeQuery();
					queryTime = System.currentTimeMillis();
				}

				rs.beforeFirst();
				while (rs.next())
				{
					String url = rs.getString("url");
					boolean active = rs.getBoolean("active");
					boolean adm_active = rs.getBoolean("adm_active");
					if (active && adm_active)
					{
						if (!threads.containsKey(url)) threads.put(url, new BotThread(client, url, conn));
						Constants.LOGGER.info("Running Thread for " + url);
						long start = System.currentTimeMillis();
						threads.get(url).run();
						Constants.LOGGER.info("Took " + (System.currentTimeMillis() - start) + " ms");
					}
				}
				Thread.sleep(5000);
			}
			catch (OAuthConnectionException | InterruptedException e)
			{
				Constants.LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}
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
