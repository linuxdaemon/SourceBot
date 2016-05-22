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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BotStatus
{
	private static final Gson gson = new Gson();
	private String stage = "unknown";
	private String currentBlog = "none";
	private int activeBlogs = 0;
	private boolean simulation = false;
	private Map<String, String> postCount = new HashMap<>();

	public void setStage(String stage) throws IOException
	{
		this.stage = stage;
		saveStateToFile();
	}

	private void saveStateToFile() throws IOException
	{
		File f = new File(SourceBot.INSTANCE.confDir, "bot.status");
		if (!f.exists()) f.createNewFile();
		FileWriter fw = new FileWriter(f);
		JsonParser parser = new JsonParser();
		JsonObject json = parser.parse(gson.toJson(this, BotStatus.class)).getAsJsonObject();
		JsonObject pcJson = new JsonObject();
		for (Map.Entry<String, String> entry : postCount.entrySet())
		{
			pcJson.add(entry.getKey(), new JsonPrimitive(entry.getValue()));
		}
		json.add("postCount", pcJson);
		fw.write(gson.toJson(json));
		fw.close();
	}

	public void setCurrentBlog(String currentBlog) throws IOException
	{
		this.currentBlog = currentBlog;
		saveStateToFile();
	}

	public void setActiveBlogs(int activeBlogs) throws IOException
	{
		this.activeBlogs = activeBlogs;
		saveStateToFile();
	}

	public void setSimulate(boolean simulate) throws IOException
	{
		this.simulation = simulate;
		saveStateToFile();
	}

	public void setPostCount(String url, String count) throws IOException
	{
		postCount.put(url, count);
		saveStateToFile();
	}
}
