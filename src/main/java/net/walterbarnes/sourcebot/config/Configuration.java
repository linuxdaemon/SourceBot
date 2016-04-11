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
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings ("SameParameterValue")
public class Configuration
{
	private static final Logger logger = Logger.getLogger(Configuration.class.getName());
	private final JsonParser parser = new JsonParser();
	private File file = null;
	private Configuration parent = null;
	private JsonObject json;
	private boolean changed = false;
	private Map<String, Configuration> children = new TreeMap<>();

	private Configuration(JsonObject json, Configuration parent)
	{
		this.json = json;
		this.parent = parent;
	}

	public Configuration(String configPath, String fileName) throws IOException
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
	}

	public void init() throws IOException
	{
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
		if (parent != null && parent != this)
		{
			return;
		}

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
			children.clear();
		}
		FileReader fr = new FileReader(this.file);
		json = parser.parse(fr).getAsJsonObject();
	}

	public boolean exists()
	{
		return this.file.exists();
	}

	public Configuration getCategory(String key, JsonObject def)
	{
		if (!json.has(key))
		{
			json.add(key, def);
			changed = true;
		}
		if (!children.containsKey(key))
		{
			children.put(key, new Configuration(json.getAsJsonObject(key), this));
		}
		return children.get(key);
	}

	public String getString(String key, String def)
	{
		if (!json.has(key))
		{
			json.add(key, new JsonPrimitive(def));
			changed = true;
		}
		return json.get(key).getAsString();
	}

	public void setString(String key, String val)
	{
		json.add(key, new JsonPrimitive(val));
	}

	public void save()
	{
		if (parent != null && parent != this)
		{
			parent.save();
			return;
		}

		try
		{
			if (file.getParentFile() != null)
			{
				if (!file.getParentFile().mkdirs())
				{
					throw new RuntimeException("Unable to save config file");
				}
			}

			if (!file.exists() && !file.createNewFile())
			{
				throw new RuntimeException("Unable to save config file");
			}

			if (file.canWrite())
			{
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				FileWriter fw = new FileWriter(this.file);
				fw.append(gson.toJson(json));
				fw.close();
			}
		}
		catch (IOException e)
		{
			logger.severe("Unable to save configuration file");
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public boolean hasChanged()
	{
		if (changed) return true;
		for (Configuration child : children.values())
		{
			if (child.hasChanged()) return true;
		}
		return false;
	}

	private void resetChangedState()
	{
		changed = false;
		for (Configuration child : children.values())
		{
			child.resetChangedState();
		}
	}
}
