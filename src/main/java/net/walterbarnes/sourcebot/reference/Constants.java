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

package net.walterbarnes.sourcebot.reference;

import com.google.gson.JsonObject;
import net.walterbarnes.sourcebot.config.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Constants
{
	private static final Logger logger = Logger.getLogger(Constants.class.getName());
	public static String version;
	public static String consumerKey, consumerSecret;
	public static String token, tokenSecret;
	public static String webRoot;

	private Constants(Configuration conf)
	{
		InputStream input = getClass().getResourceAsStream("/sbversion.properties");
		Properties properties = new Properties();
		if (input != null)
		{
			try
			{
				properties.load(input);
			}
			catch (IOException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		version = properties.getProperty("sbbuild.version.number", "missing");

		Configuration tumblrCat = conf.getCategory("tumblr", new JsonObject());

		consumerKey = tumblrCat.getString("consumerKey", "missing");
		consumerSecret = tumblrCat.getString("consumerSecret", "missing");

		token = tumblrCat.getString("token", "missing");
		tokenSecret = tumblrCat.getString("tokenSecret", "missing");

		Configuration webCat = conf.getCategory("webapp", new JsonObject());

		webRoot = webCat.getString("webRoot", "localhost");
	}

	public static void load(Configuration conf)
	{
		new Constants(conf);
	}
}
