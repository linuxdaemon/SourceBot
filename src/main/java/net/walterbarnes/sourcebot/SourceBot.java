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

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.Blog;
import net.walterbarnes.sourcebot.cli.Cli;
import net.walterbarnes.sourcebot.crash.CrashReport;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;
import net.walterbarnes.sourcebot.tumblr.Tumblr;
import net.walterbarnes.sourcebot.util.LogHelper;
import org.scribe.exceptions.OAuthConnectionException;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class SourceBot
{
	private static final File confDir = new File(System.getProperty("user.home"), ".sourcebot");
	private static final String jsonName = "SourceBot.json";
	private static Logger logger = Logger.getLogger(SourceBot.class.getName());
	private static JsonParser parser = new JsonParser();
	private static JsonObject json;
	private static String[] args;
	private static List<Blog> blogs;

	public static void main(String[] args)
	{
		SourceBot.args = args;
		CrashReport crashreport;
		try
		{
			LogHelper.init(SourceBot.class);

			if (!confDir.exists())
			{ confDir.mkdirs(); }
			File jsonFile = new File(confDir, jsonName);
			logger.info(jsonFile.getAbsolutePath());

			if (Arrays.asList(args).contains("install") || !jsonFile.exists())
			{
				if (!jsonFile.createNewFile())
				{ throw new RuntimeException("Unable to Create Configuration File"); }
				FileReader fr = new FileReader(jsonFile);
				json = parser.parse(new FileReader(jsonFile)).getAsJsonObject();
				fr.close();
				initJson();
				JsonObject db = json.getAsJsonObject("db");
				initDb(db.get("host").getAsString(), db.get("user").getAsString(), db.get("pass").getAsString(),
						db.get("db_name").getAsString());
				System.exit(0);
			}
			FileReader fr = new FileReader(jsonFile);
			json = parser.parse(new FileReader(jsonFile)).getAsJsonObject();
			fr.close();
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			run();
		}
		catch (Throwable throwable)
		{
			crashreport = new CrashReport("Unexpected error", throwable);
			displayCrashReport(crashreport);
		}
	}

	private static void initJson()
	{
		if (Cli.promptYesNo("First Run?[Y/n]: "))
		{
			String consumerKey = "";
			String consumerSecret = "";
			String token = "";
			String tokenSecret = "";

			boolean validOauth = false;
			while (!validOauth)
			{
				try
				{
					consumerKey = Cli.prompt("[Tumblr API] Consumer Key: ", Pattern.compile("[0-9A-Za-z]+"));
					consumerSecret = Cli.prompt("[Tumblr API] Consumer Secret: ", Pattern.compile("[0-9A-Za-z]+"));
					token = Cli.prompt("[Tumblr API] Token: ", Pattern.compile("[0-9A-Za-z]+"));
					tokenSecret = Cli.prompt("[Tumblr API] Token Secret: ", Pattern.compile("[0-9A-Za-z]+"));
					Tumblr tumblr = new Tumblr(consumerKey, consumerSecret, token, tokenSecret, logger);
					blogs = tumblr.user().getBlogs();
					validOauth = true;
				}
				catch (JumblrException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}

			String dbHost = Cli.prompt("[Database] Hostname: ", Pattern.compile(".+"));
			String dbUser = Cli.prompt("[Database] Username: ", Pattern.compile("[^ ]+"));
			String dbPass = Cli.password("[Database] Password: ", Pattern.compile(".+"));
			String dbName = Cli.prompt("[Database] Database Name: ", Pattern.compile(".*"));

			File jsonFile;
			FileWriter fw = null;
			FileReader fr = null;
			try
			{
				if (!(jsonFile = new File(confDir, jsonName)).exists())
				{
					if (!jsonFile.createNewFile())
					{
						throw new RuntimeException("Unable to Create Config File");
					}
					fw = new FileWriter(jsonFile);
					JsonWriter pjw = new JsonWriter(fw);
					pjw.beginObject();
					pjw.endObject();
					pjw.close();
					fw.close();
				}
				fr = new FileReader(jsonFile);
				json = parser.parse(new FileReader(jsonFile)).getAsJsonObject();
				fr.close();
				json.add("db", new JsonObject());

				JsonObject db = json.getAsJsonObject("db");
				db.add("host", new JsonPrimitive(dbHost));
				db.add("user", new JsonPrimitive(dbUser));
				db.add("pass", new JsonPrimitive(dbPass));
				db.add("db_name", new JsonPrimitive(dbName));

				json.add("api", new JsonObject());
				JsonObject api = json.getAsJsonObject("api");

				api.add("consumer", new JsonObject());
				JsonObject consumer = api.getAsJsonObject("consumer");
				consumer.add("key", new JsonPrimitive(consumerKey));
				consumer.add("secret", new JsonPrimitive(consumerSecret));

				api.add("token", new JsonObject());
				JsonObject tkn = api.getAsJsonObject("token");
				tkn.add("key", new JsonPrimitive(token));
				tkn.add("secret", new JsonPrimitive(tokenSecret));

				fw = new FileWriter(jsonFile);
				Gson gsonBuilder = new GsonBuilder().setPrettyPrinting().create();
				fw.append(gsonBuilder.toJson(json));
				fw.close();
			}
			catch (IOException e)
			{
				logger.severe("File Error");
				logger.log(Level.SEVERE, e.getMessage(), e);
				System.exit(1);
			}
			finally
			{
				if (fw != null) try { fw.close(); } catch (IOException ignored) {}
				if (fr != null) try { fr.close(); } catch (IOException ignored) {}
			}
		}
	}

	private static void initDb(String dbHost, String dbUser, String dbPass, String dbName)
	{
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		}
		catch (IllegalAccessException e)
		{
			logger.severe("Unable to Access MySQL Driver Classes, Exiting...");
			System.exit(1);
		}
		catch (InstantiationException e)
		{
			logger.severe("Unable to Instantiate MySQL Driver, Exiting...");
			System.exit(1);
		}
		catch (ClassNotFoundException e)
		{
			logger.severe("Unable to load MySQL Driver, Exiting...");
			System.exit(1);
		}
		logger.info("Connecting to Database Server...");
		Connection conn;
		try
		{
			conn = DriverManager.getConnection(String.format("jdbc:mysql://%s", dbHost), dbUser, dbPass);
			logger.info("Connected to Server.");

			logger.info("Creating Database if it doesn't exist...");
			PreparedStatement createDb = conn.prepareStatement(String.format("CREATE DATABASE IF NOT EXISTS %s;", dbName));
			createDb.execute();
			logger.info("Done.");

			logger.info("Disconnecting From Server....");
			conn.close();
			logger.info("Disconnected...");

			logger.info("Reconnecting to Server with New Database...");
			conn = DriverManager.getConnection(String.format("jdbc:mysql://%s/%s", dbHost, dbName), dbUser, dbPass);
			logger.info("Connected.");

			logger.info("Checking Existence of Required Tables...");
			PreparedStatement blogsCheck = conn.prepareStatement("SHOW TABLES LIKE 'blogs';");
			PreparedStatement blogsCreate = conn.prepareStatement("CREATE TABLE `blogs` (" +
					"`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
					"`url` TEXT NOT NULL," +
					"`blog_check_active` BOOL NOT NULL DEFAULT TRUE," +
					"`sample_size` INT NOT NULL DEFAULT '1000'," +
					"`post_type` TEXT NOT NULL," +
					"`post_select` TEXT NOT NULL," +
					"`post_state` TEXT NOT NULL," +
					"`post_buffer` INT NOT NULL DEFAULT '20'," +
					"`post_comment` TEXT," +
					"`post_tags` TEXT," +
					"`active` BOOL NOT NULL DEFAULT FALSE" +
					")");

			PreparedStatement rulesCheck = conn.prepareStatement("SHOW TABLES LIKE 'search_rules'");
			PreparedStatement rulesCreate = conn.prepareStatement("CREATE TABLE `search_rules` (" +
					"`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
					"`url` TEXT NOT NULL," +
					"`type` TEXT NOT NULL," +
					"`action` TEXT NOT NULL," +
					"`term` TEXT NOT NULL" +
					")");

			PreparedStatement postsCheck = conn.prepareStatement("SHOW TABLES LIKE 'seen_posts'");
			PreparedStatement postsCreate = conn.prepareStatement("CREATE TABLE `seen_posts` (" +
					"`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
					"`url` TEXT NOT NULL," +
					"`post_id` BIGINT NOT NULL," +
					"`tag` TEXT NOT NULL," +
					"`blog` TEXT NOT NULL," +
					"`rb_id` BIGINT NOT NULL" +
					")");

			PreparedStatement statsCheck = conn.prepareStatement("SHOW TABLES LIKE 'tag_stats'");
			PreparedStatement statsCreate = conn.prepareStatement("CREATE TABLE `tag_stats` (" +
					"`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
					"`url` TEXT NOT NULL," +
					"`tag` TEXT NOT NULL," +
					"`time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
					"`search_time` INT NOT NULL," +
					"`search` INT NOT NULL," +
					"`selected` INT NOT NULL" +
					")");

			if (!blogsCheck.executeQuery().next())
			{
				logger.info("Blogs Table Doesn't Exist, Creating...");
				blogsCreate.execute();
				logger.info("Created.");
			}
			if (!rulesCheck.executeQuery().next())
			{
				logger.info("Rules Table Doesn't Exist, Creating...");
				rulesCreate.execute();
				logger.info("Created.");
			}
			if (!postsCheck.executeQuery().next())
			{
				logger.info("Posts Table Doesn't Exist, Creating...");
				postsCreate.execute();
				logger.info("Created.");
			}
			if (!statsCheck.executeQuery().next())
			{
				logger.info("Stats Table Doesn't Exist, Creating...");
				statsCreate.execute();
				logger.info("Created.");
			}
			logger.info("Creating First Blog Configuration...");
			PreparedStatement firstBlog = conn.prepareStatement("INSERT INTO blogs (id,url,blog_check_active,sample_size,post_type,post_select,post_state,post_buffer,post_comment,post_tags,active) VALUES (?,?,?,?,?,?,?,?,?,?,?);");
			firstBlog.setInt(1, 1);
			boolean valid = false;
			String url = "";
			while (!valid)
			{
				url = Cli.prompt("[Config] Blog Name: ", Pattern.compile("[^ ]+"));
				logger.info("Checking URL...");
				if (blogs == null)
				{
					JsonObject api = json.getAsJsonObject("api");
					JsonObject consumer = api.getAsJsonObject("consumer");
					JsonObject token = api.getAsJsonObject("token");
					Tumblr tumblr = new Tumblr(consumer.get("key").getAsString(), consumer.get("secret").getAsString(),
							token.get("key").getAsString(), token.get("secret").getAsString(), logger);
					blogs = tumblr.user().getBlogs();
				}
				for (Blog b : blogs) if (url != null && b.getName().equals(url)) valid = true;
				if (!valid) System.out.println("URL Not Registered to Your Account");
			}
			firstBlog.setString(2, url);
			firstBlog.setBoolean(3, Cli.promptYesNo("[Config] Require Blogs to be Active (5 or more posts) Before Reblogging From Them?[Y/n]: "));
			firstBlog.setInt(4, Cli.promptInt("[Config] Post Sample Size?(per tag)[1000]: ", 1000));
			Map<String, String> opts = new LinkedHashMap<>();
			opts.put("All", "null");
			opts.put("Text", "text");
			opts.put("Quote", "quote");
			opts.put("Link", "link");
			opts.put("Photo", "photo");
			opts.put("Answer", "answer");
			opts.put("Chat", "chat");

			firstBlog.setString(5, Cli.promptList("[Config] Post Type: ", opts));
			opts.clear();
			opts.put("Select Top 50 Posts by Notes", "top");
			opts.put("Select Top 50 Posts by Time Posted", "recent");
			firstBlog.setString(6, Cli.promptList("[Config] Post Selection Method: ", opts));
			opts.clear();
			opts.put("Queued", "queue");
			opts.put("Drafted", "draft");
			firstBlog.setString(7, Cli.promptList("[Config] Reblogged Post State: ", opts));
			opts.clear();
			firstBlog.setInt(8, Cli.promptInt("[Config] Number of Posts to Keep in Queue/Drafts[20]: ", 20));
			firstBlog.setString(9, Cli.prompt("[Config] Comment to Add to All Reblogged Posts[]: ",
					Pattern.compile(".*")).trim());
			firstBlog.setString(10, Cli.prompt("[Config] Tag to Add to All Reblogged Posts[]: ",
					Pattern.compile("[^,]*")).trim());
			firstBlog.setBoolean(11, false);
			firstBlog.execute();
			logger.info("Created.");
			System.out.println("Now Add Search Rules Before Activating The Blog");
			logger.info("Disconnecting from Database Server...");
			conn.close();
			logger.info("Disconnected.");
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			System.exit(1);
		}

	}

	private static void run() throws InvalidBlogNameException, SQLException, FileNotFoundException,
			InstantiationException, IllegalAccessException
	{
		JsonObject api = json.getAsJsonObject("api");
		JsonObject consumer = api.getAsJsonObject("consumer");
		JsonObject token = api.getAsJsonObject("token");

		JsonObject db = json.getAsJsonObject("db");

		Tumblr client = new Tumblr(consumer.get("key").getAsString(), consumer.get("secret").getAsString(),
				token.get("key").getAsString(), token.get("secret").getAsString(), logger);

		Connection conn = DriverManager.getConnection("jdbc:mysql://" + db.get("host").getAsString() + "/" +
				db.get("db_name").getAsString(), db.get("user").getAsString(), db.get("pass").getAsString());

		PreparedStatement getBlogs = conn.prepareStatement("SELECT DISTINCT url,active FROM blogs ORDER BY id;");
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
					if (active)
					{
						if (!threads.containsKey(url)) threads.put(url, new BotThread(client, url, conn));
						logger.info("Running Thread for " + url);
						long start = System.currentTimeMillis();
						threads.get(url).run();
						logger.info("Took " + (System.currentTimeMillis() - start) + " ms");
					}
				}
				Thread.sleep(5000);
			}
			catch (OAuthConnectionException | InterruptedException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
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
