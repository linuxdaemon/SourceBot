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

package net.walterbarnes.sourcebot.config;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import net.walterbarnes.sourcebot.SourceBot;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

@SuppressWarnings ("SameParameterValue")
public class Config
{
	private final JsonParser parser = new JsonParser();
	private File file = null;
	private Config parent = null;
	private JsonObject json;

	public Config(String configPath, String fileName) throws IOException
	{
		File configDir = new File(configPath);
		if (!configDir.exists())
		{
			if (!configDir.mkdirs())
			{
				throw new RuntimeException("Unable to create config dir '" + configDir.getAbsolutePath() + "'");
			}
		}
		this.file = new File(configDir, fileName);
		try
		{
			load();
		}
		catch (Throwable e)
		{
			File fileBak = new File(file.getAbsolutePath() + "_" +
					new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".errored");
			Logger logger = Logger.getLogger(SourceBot.class.getName());
			logger.warning("Encountered an error while loading configuration, generating new config");
			if (!file.renameTo(fileBak))
			{
				throw new RuntimeException("Unable to back up config file");
			}
			load();
		}
	}

	private void load() throws IOException
	{
		if (!file.canRead())
		{
			throw new RuntimeException("Unable to read config file, invalid permissions");
		}
		if (!file.canWrite())
		{
			throw new RuntimeException("Unable to write to config file, invalid permissions");
		}
		if (!file.exists())
		{
			if (!file.createNewFile())
			{
				throw new RuntimeException("Unable to create new config file");
			}
			FileWriter fw = new FileWriter(this.file);
			JsonWriter pjw = new JsonWriter(fw);
			pjw.beginObject();
			pjw.endObject();
			pjw.close();
			fw.close();
		}
		FileReader fr = new FileReader(this.file);
		json = parser.parse(fr).getAsJsonObject();
	}

	private Config(JsonObject json, Config parent)
	{
		this.json = json;
		this.parent = parent;
	}

	public Config getCategory(String key, JsonObject def)
	{
		if (!json.has(key))
		{
			json.add(key, def);
		}
		return new Config(json.getAsJsonObject(key), this);
	}

	public String getString(String key, String def)
	{
		if (!json.has(key))
		{
			json.add(key, new JsonPrimitive(def));
		}
		return json.get(key).getAsString();
	}

	public void setString(String key, String val)
	{
		json.add(key, new JsonPrimitive(val));
	}

	public void save() throws IOException
	{
		if (parent == null)
		{
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			FileWriter fw = new FileWriter(this.file);
			fw.append(gson.toJson(json));
			fw.close();
		}
		else
		{
			parent.save();
		}
	}
}
