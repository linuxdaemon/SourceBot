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

package net.walterbarnes.sourcebot.bot.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class Configuration
{
	public final String dbHost;
	public final String dbPort;
	public final String dbUser;
	public final String dbPass;
	public final String dbName;
	public final String consumerKey;
	public final String consumerSecret;
	public final String token;
	public final String tokenSecret;
	private final JsonParser parser = new JsonParser();
	private JsonObject json;

	public Configuration(@Nonnull String configPath, @Nonnull String fileName) throws FileNotFoundException
	{
		File configDir = new File(configPath);
		File config = new File(configDir, fileName);
		json = parser.parse(new FileReader(config)).getAsJsonObject();
		
		JsonObject dbConf = json.getAsJsonObject("db");
		dbHost = dbConf.get("host").getAsString();
		dbPort = dbConf.get("port").getAsString();
		dbUser = dbConf.get("user").getAsString();
		dbPass = dbConf.get("pass").getAsString();
		dbName = dbConf.get("dbName").getAsString();
		JsonObject tmblrConf = json.getAsJsonObject("tumblr");

		consumerKey = tmblrConf.get("consumerKey").getAsString();
		consumerSecret = tmblrConf.get("consumerSecret").getAsString();
		token = tmblrConf.get("token").getAsString();
		tokenSecret = tmblrConf.get("tokenSecret").getAsString();
	}
}

