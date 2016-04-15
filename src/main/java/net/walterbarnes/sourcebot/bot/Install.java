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

import com.github.onyxfoxdevelopment.cli.Prompt;
import com.google.gson.JsonObject;
import com.tumblr.jumblr.exceptions.JumblrException;
import net.walterbarnes.sourcebot.common.config.Configuration;
import net.walterbarnes.sourcebot.common.tumblr.Tumblr;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@SuppressWarnings ({"UnusedReturnValue", "SameReturnValue"})
public class Install
{
	private static final Logger logger = Logger.getLogger(Install.class.getName());
	
	public static boolean install(String path, String name)
	{
		Configuration conf = new Configuration(path, name);
		Configuration apiCat = conf.getCategory("api", new JsonObject());
		Configuration consumerCat = apiCat.getCategory("consumer", new JsonObject());
		Configuration tokenCat = apiCat.getCategory("token", new JsonObject());

		int failCount = 0;
		boolean validOauth = false;
		while (!validOauth && failCount < 10)
		{
			try
			{
				String consumerKey = consumerCat.getString("key", "");
				String consumerSecret = consumerCat.getString("secret", "");
				String token = tokenCat.getString("key", "");
				String tokenSecret = tokenCat.getString("secret", "");
				if (consumerKey.isEmpty())
				{
					consumerKey = Prompt.prompt("[Tumblr API] Consumer Key: ", Pattern.compile("[0-9A-Za-z]+"));
					consumerCat.setString("key", consumerKey);
				}
				if (consumerSecret.isEmpty())
				{
					consumerSecret = Prompt.prompt("[Tumblr API] Consumer Secret: ", Pattern.compile("[0-9A-Za-z]+"));
					consumerCat.setString("secret", consumerSecret);
				}
				if (token.isEmpty())
				{
					token = Prompt.prompt("[Tumblr API] Token: ", Pattern.compile("[0-9A-Za-z]+"));
					tokenCat.setString("key", token);
				}
				if (tokenSecret.isEmpty())
				{
					tokenSecret = Prompt.prompt("[Tumblr API] Token Secret: ", Pattern.compile("[0-9A-Za-z]+"));
					tokenCat.setString("secret", tokenSecret);
				}
				Tumblr tumblr = new Tumblr(consumerKey, consumerSecret, token, tokenSecret);
				tumblr.user().getBlogs();
				validOauth = true;
			}
			catch (JumblrException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				logger.warning("!!Invalid API Authentication!!");
				failCount++;
			}
		}
		if (!validOauth)
		{ throw new RuntimeException("Unable to Install SourceBot, too many failed attempts"); }
		installDb(conf);
		conf.save();
		return true;
	}

	private static boolean installDb(Configuration config)
	{
		Configuration dbCat = config.getCategory("db", new JsonObject());
		String dbHost = Prompt.prompt("[Database] Hostname[localhost]: ", Pattern.compile(".+"), "localhost");
		String dbPort = Prompt.prompt("[Database] Port[5432]: ", Pattern.compile(".+"), "5432");
		String dbUser = Prompt.prompt("[Database] Username[sourcebot]: ", Pattern.compile("[^ ]+"), "sourcebot");
		String dbPass = Prompt.password("[Database] Password: ", Pattern.compile(".+"));
		String dbName = Prompt.prompt("[Database] Database Name[sourcebot]: ", Pattern.compile("[0-9a-zA-Z$_]+"), "sourcebot");
		dbCat.setString("host", dbHost);
		dbCat.setString("port", dbPort);
		dbCat.setString("user", dbUser);
		dbCat.setString("pass", dbPass);
		dbCat.setString("db_name", dbName);

		try
		{
			Class.forName("org.postgresql.Driver").newInstance();
		}
		catch (IllegalAccessException e)
		{
			logger.severe("Unable to Access MySQL Driver Classes, Exiting...");
			throw new RuntimeException("Unable to install SourceBot, Database error occurred");
		}
		catch (InstantiationException e)
		{
			logger.severe("Unable to Instantiate MySQL Driver, Exiting...");
			throw new RuntimeException("Unable to install SourceBot, Database error occurred");
		}
		catch (ClassNotFoundException e)
		{
			logger.severe("Unable to load MySQL Driver, Exiting...");
			throw new RuntimeException("Unable to install SourceBot, Database error occurred");
		}
		logger.info("Connecting to Database Server...");
		Connection conn = null;
		Statement createDb = null;
		try
		{
			conn = DriverManager.getConnection(String.format("jdbc:postgresql://%s:%s", dbHost, dbPort), dbUser, dbPass);
			logger.info("Connected to Server.");

			logger.info("Creating Database if it doesn't exist...");
			createDb = conn.createStatement();
			createDb.execute("CREATE DATABASE IF NOT EXISTS " + dbName);
			logger.info("Done.");

			logger.info("Disconnecting From Server...");
			conn.close();
			logger.info("Disconnected.");

			logger.info("Reconnecting to Server with New Database...");
			conn = DriverManager.getConnection(String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName), dbUser, dbPass);
			logger.info("Connected.");
			checkDbTables(conn);
			logger.info("Disconnecting from Database Server...");
			conn.close();
			logger.info("Disconnected.");
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw new RuntimeException("Unable to complete installation, database error occurred");
		}
		finally
		{
			if (createDb != null)
			{
				try
				{
					createDb.close();
				}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
		return true;
	}

	private static void checkDbTables(Connection conn) throws SQLException
	{
		PreparedStatement blogsCreate = null;
		PreparedStatement searchExcCreate = null;
		PreparedStatement searchIncCreate = null;
		PreparedStatement searchStatsCreate = null;
		PreparedStatement postsCreate = null;
		try
		{
			logger.info("Checking Existence of Required Tables...");
			blogsCreate = conn.prepareStatement("CREATE TABLE IF NOT EXISTS blogs (" +
					"id SERIAL PRIMARY KEY NOT NULL," +
					"url TEXT NOT NULL," +
					"blog_check_active BOOLEAN NOT NULL DEFAULT TRUE," +
					"sample_size INTEGER NOT NULL DEFAULT 1000," +
					"post_type TEXT NOT NULL," +
					"post_select TEXT NOT NULL," +
					"post_state TEXT NOT NULL," +
					"post_buffer INTEGER NOT NULL DEFAULT 20," +
					"post_comment TEXT," +
					"post_tags TEXT[]," +
					"preserve_tags BOOLEAN NOT NULL DEFAULT FALSE," +
					"active BOOLEAN NOT NULL DEFAULT FALSE" +
					");");

			searchExcCreate = conn.prepareStatement("CREATE TABLE IF NOT EXISTS search_exclusions (" +
					"id SERIAL PRIMARY KEY NOT NULL," +
					"blog_id INTEGER NOT NULL," +
					"type TEXT NOT NULL," +
					"term TEXT NOT NULL," +
					"active BOOLEAN NOT NULL DEFAULT TRUE" +
					");");

			searchIncCreate = conn.prepareStatement("CREATE TABLE IF NOT EXISTS search_inclusions (" +
					"id SERIAL PRIMARY KEY NOT NULL," +
					"blog_id INTEGER NOT NULL," +
					"type TEXT NOT NULL," +
					"term TEXT NOT NULL," +
					"required_tags TEXT[]," +
					"post_type TEXT," +
					"post_select TEXT," +
					"sample_size INTEGER," +
					"active BOOLEAN NOT NULL DEFAULT TRUE" +
					");");

			searchStatsCreate = conn.prepareStatement("CREATE TABLE IF NOT EXISTS search_stats (" +
					"id SERIAL PRIMARY KEY NOT NULL," +
					"url TEXT NOT NULL," +
					"type TEXT NOT NULL," +
					"term TEXT NOT NULL," +
					"time TIMESTAMP NOT NULL DEFAULT now()," +
					"search_time INTEGER NOT NULL," +
					"searched INTEGER NOT NULL," +
					"selected INTEGER NOT NULL" +
					");");

			postsCreate = conn.prepareStatement("CREATE TABLE IF NOT EXISTS seen_posts (" +
					"id SERIAL PRIMARY KEY NOT NULL," +
					"url TEXT NOT NULL," +
					"search_type TEXT NOT NULL," +
					"search_term TEXT NOT NULL," +
					"post_id BIGINT NOT NULL," +
					"blog TEXT NOT NULL," +
					"rb_id BIGINT NOT NULL" +
					");");

			logger.info("Creating blogs table if it doesn't exist...");
			blogsCreate.execute();
			logger.info("Done.");

			logger.info("Creating search_exclusions table if it doesn't exist...");
			searchExcCreate.execute();
			logger.info("Done.");

			logger.info("Creating search_inclusions table if it doesn't exist...");
			searchIncCreate.execute();
			logger.info("Done.");

			logger.info("Creating search_stats table if it doesn't exist...");
			searchStatsCreate.execute();
			logger.info("Done.");

			logger.info("Creating seen_posts table if it doesn't exist...");
			postsCreate.execute();
			logger.info("Done.");
		}
		finally
		{
			if (blogsCreate != null)
			{
				try
				{
					blogsCreate.close();
				}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			if (searchExcCreate != null)
			{
				try
				{
					searchExcCreate.close();
				}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			if (searchIncCreate != null)
			{
				try
				{
					searchIncCreate.close();
				}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			if (searchStatsCreate != null)
			{
				try
				{
					searchStatsCreate.close();
				}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			if (postsCreate != null)
			{
				try
				{
					postsCreate.close();
				}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}

}
