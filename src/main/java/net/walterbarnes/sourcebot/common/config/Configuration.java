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

package net.walterbarnes.sourcebot.common.config;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import net.walterbarnes.sourcebot.bot.SourceBot;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.Charset;
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
	private final Map<String, Configuration> children = new TreeMap<>();
	private File file = null;
	private Configuration parent = null;
	private JsonObject json;
	private boolean changed = false;

	private Configuration(@Nonnull JsonObject json, @Nonnull Configuration parent)
	{
		this.json = json;
		this.parent = parent;
	}

	public Configuration(@Nonnull String configPath, @Nonnull String fileName)
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
					new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".error");
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
		InputStreamReader fr = null;
		try
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
				OutputStreamWriter fw = new OutputStreamWriter(
						new FileOutputStream(this.file),
						Charset.forName("UTF-8").newEncoder()
				);
				JsonWriter pjw = new JsonWriter(fw);
				pjw.beginObject();
				pjw.endObject();
				pjw.close();
				fw.close();
				children.clear();
			}
			fr = new InputStreamReader(
					new FileInputStream(file),
					Charset.forName("UTF-8").newDecoder()
			);
			json = parser.parse(fr).getAsJsonObject();
			fr.close();
			resetChangedState();
		}
		finally
		{
			if (fr != null)
			{
				try
				{
					fr.close();
				}
				catch (IOException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}

	private void resetChangedState()
	{
		changed = false;
		children.values().forEach(Configuration::resetChangedState);
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

	@SuppressWarnings ("ResultOfMethodCallIgnored")
	public void save()
	{
		if (parent != null && parent != this)
		{
			parent.save();
			return;
		}

		OutputStreamWriter fw = null;
		try
		{
			if (file.getParentFile() != null)
			{
				if (!file.exists() && !file.getParentFile().mkdirs())
				{
					throw new RuntimeException("Unable to create config dir");
				}
			}

			if (!file.exists() && !file.createNewFile())
			{
				throw new RuntimeException("Unable to save config file");
			}

			if (file.canWrite())
			{
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				fw = new OutputStreamWriter(
						new FileOutputStream(file),
						Charset.forName("UTF-8").newEncoder()
				);
				fw.append(gson.toJson(json));
				fw.close();
			}
		}
		catch (IOException e)
		{
			logger.severe("Unable to save configuration file");
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		finally
		{
			if (fw != null)
			{
				try
				{
					fw.close();
				}
				catch (IOException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
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
}
