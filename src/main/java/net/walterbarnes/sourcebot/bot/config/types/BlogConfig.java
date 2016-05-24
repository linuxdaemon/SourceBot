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

package net.walterbarnes.sourcebot.bot.config.types;

import net.walterbarnes.sourcebot.bot.config.DB;
import net.walterbarnes.sourcebot.bot.search.SearchExclusion;
import net.walterbarnes.sourcebot.bot.search.SearchInclusion;
import net.walterbarnes.sourcebot.bot.search.SearchRule;
import net.walterbarnes.sourcebot.bot.tumblr.Tumblr;
import net.walterbarnes.sourcebot.bot.util.LogHelper;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings ("UnusedReturnValue")
public class BlogConfig
{
	private final Tumblr client;
	private final Collection<SearchRule> rules = new ArrayList<>();
	private final Connection connection;
	private final String id;
	private String url;
	private long rulesQTime = 0;
	private long configQTime = 0;
	private boolean active;
	private boolean blogCheckActive;
	private int sampleSize;
	private String[] postType;
	private String postSelect;
	private String postState;
	private int postBuffer;
	private String postComment;
	private String[] postTags;
	private boolean preserveTags;
	private boolean admActive;

	public BlogConfig(@Nonnull Tumblr client, @Nonnull Connection connection, @Nonnull String id) throws SQLException
	{
		this.id = id;
		this.client = client;
		this.connection = connection;

		try (PreparedStatement getUrl = connection.prepareStatement("SELECT url FROM blogs WHERE id = ?::UUID"))
		{
			getUrl.setString(1, id);
			try (ResultSet rs = getUrl.executeQuery())
			{
				boolean firstRun = true;
				String url = "";

				while (rs.next())
				{
					if (!firstRun)
						throw new RuntimeException("Multiple blogs exist with that blog id '" + id + "'");
					firstRun = false;
					url = rs.getString("url");
				}

				if (url.isEmpty())
				{
					throw new RuntimeException("No blogs exist with blog id '" + id + "'");
				}
				this.url = url;
			}
		}
	}

	public Tumblr getClient()
	{
		return client;
	}
	
	public boolean isPostBufFull()
	{
		if (getPostState().equals("queue") && !client.blogInfo(url).isAdmin())
		{
			LogHelper.warn("Bot is not admin on '" + url + "', not running thread");
			return true;
		}
		if (getPostState().equals("draft"))
		{
			return client.getDrafts(url).size() >= getPostBuffer();
		}
		return client.getQueuedPosts(url).size() >= getPostBuffer();
	}

	public String getPostState()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return String.valueOf(this.postState);
	}

	private int getPostBuffer()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return this.postBuffer;
	}

	private void loadConfig()
	{
		try (PreparedStatement st = connection.prepareStatement("SELECT * FROM blogs WHERE id = ?::UUID"))
		{
			LogHelper.info("Loading config for" + url);
			st.setString(1, id);
			try (ResultSet rs = st.executeQuery())
			{
				boolean firstRun = true;
				while (rs.next())
				{
					if (!firstRun)
						throw new RuntimeException("Multiple blogs exists with id '" + id + "'");
					firstRun = false;
					this.url = rs.getString("url");
					this.blogCheckActive = rs.getBoolean("blog_check_active");
					this.sampleSize = rs.getInt("sample_size");
					Array postType = rs.getArray("post_type");
					try { this.postType = (String[]) postType.getArray(); }
					catch (SQLException ignored) { this.postType = new String[0]; }
					this.postSelect = rs.getString("post_select");
					this.postState = rs.getString("post_state");
					this.postBuffer = rs.getInt("post_buffer");
					this.postComment = rs.getString("post_comment");
					Array postTags = rs.getArray("post_tags");
					try { this.postTags = (String[]) postTags.getArray(); }
					catch (SQLException ignored) { this.postTags = new String[0]; }
					this.preserveTags = rs.getBoolean("preserve_tags");
					this.active = rs.getBoolean("active");
					this.admActive = rs.getBoolean("adm_active");
				}
				configQTime = System.currentTimeMillis();
			}
		}
		catch (SQLException e)
		{
			LogHelper.error(e);
		}
	}

	public String getPostBufSize()
	{
		if (getPostState().equals("queue") && !client.blogInfo(url).isAdmin())
		{
			LogHelper.warn("Bot is not admin on '" + url + "', not running thread");
			return "error";
		}
		if (getPostState().equals("draft"))
		{
			return String.valueOf(client.getDrafts(url).size());
		}
		return String.valueOf(client.getQueuedPosts(url).size());
	}

	public int getBufferSize()
	{
		if (getPostState().equals("draft"))
		{
			return client.getDrafts(url).size();
		}
		if (!client.blogInfo(url).isAdmin())
		{
			LogHelper.warn("Bot is not admin on '" + url + "', not running thread");
			return 0;
		}
		return client.getQueuedPosts(url).size();
	}

	public boolean addStat(String type, String tag, int time, int searched, int selected)
	{
		try (PreparedStatement addStats = connection.prepareStatement("INSERT INTO search_stats (blog_id, type, term, search_time, searched, selected) VALUES (?::UUID, ?, ?, ?, ?, ?)"))
		{
			addStats.setString(1, id);
			addStats.setString(2, type);
			addStats.setString(3, tag);
			addStats.setInt(4, time);
			addStats.setInt(5, searched);
			addStats.setInt(6, selected);
			return addStats.execute();
		}
		catch (SQLException e)
		{
			LogHelper.error(e);
			throw new RuntimeException("Database error occurred, exiting...");
		}
	}

	public boolean addPost(String type, long id, long rbId, String tag, String blogName)
	{
		try (PreparedStatement addPosts = connection.prepareStatement("INSERT INTO seen_posts (blog_id, search_type, post_id, rb_id, search_term, blog) VALUES (?::UUID, ?, ?, ?, ?, ?)"))
		{
			addPosts.setString(1, this.id);
			addPosts.setString(2, type);
			addPosts.setLong(3, id);
			addPosts.setLong(4, rbId);
			addPosts.setString(5, tag);
			addPosts.setString(6, blogName);
			return addPosts.execute();
		}
		catch (SQLException e)
		{
			LogHelper.error(e);
			throw new RuntimeException("Database error occurred, exiting...");
		}
	}

	public List<Long> getPosts()
	{
		List<Long> out = new ArrayList<>();
		try (PreparedStatement getPosts = connection.prepareStatement("SELECT post_id FROM seen_posts WHERE blog_id = ?::UUID"))
		{
			getPosts.setString(1, this.id);
			try (ResultSet rs = getPosts.executeQuery())
			{
				while (rs.next()) out.add(rs.getLong("post_id"));
			}
		}
		catch (SQLException e)
		{
			LogHelper.error(e);
			throw new RuntimeException("Database error occurred, exiting...");
		}
		return out;
	}

	public String[] getPostType()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return this.postType.clone();
	}

	public boolean getCheckBlog()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return this.blogCheckActive;
	}

	public boolean getPreserveTags()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return this.preserveTags;
	}

	public String getPostSelect()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return String.valueOf(this.postSelect);
	}

	public String getPostComment()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return String.valueOf(this.postComment);
	}

	public String[] getPostTags()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return this.postTags.clone();
	}

	public int getSampleSize()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return this.sampleSize;
	}

	public Collection<SearchRule> getSearchRules()
	{
		if (System.currentTimeMillis() - rulesQTime > DB.getCacheTime())
		{
			Collection<SearchRule> out = new ArrayList<>();
			try (PreparedStatement getRules = connection.prepareStatement("SELECT 'include' AS action,* FROM search_inclusions WHERE blog_id = ?::UUID UNION ALL SELECT 'exclude' AS action,id,blog_id,type,term,NULL,NULL,NULL,NULL,active,modified FROM search_exclusions WHERE blog_id = ?::UUID ORDER BY term"))
			{
				getRules.setString(1, id);
				getRules.setString(2, id);
				try (ResultSet rs = getRules.executeQuery())
				{
					while (rs.next())
					{
						String action = rs.getString("action");
						int id = rs.getInt("id");
						String blogId = rs.getString("blog_id");
						String type = rs.getString("type");
						String term = rs.getString("term");
						String[] requiredTags = rs.getArray("required_tags") != null ? (String[]) rs.getArray("required_tags").getArray() : null;
						String[] postType = rs.getArray("post_type") != null ? (String[]) rs.getArray("post_type").getArray() : null;
						String postSelect = rs.getString("post_select");
						int sample = rs.getInt("sample_size");
						boolean active = rs.getBoolean("active");
						Timestamp modified = rs.getTimestamp("modified");
						switch (action)
						{
							case "include":
								out.add(new SearchInclusion(this, id, blogId, type, term, requiredTags, postType, postSelect, sample, active, modified.getTime()));
								break;
							case "exclude":
								out.add(new SearchExclusion(this, id, blogId, type, term, active, modified.getTime()));
								break;
							default:
								throw new RuntimeException(String.format("Unknown search rule in database, id '%s_%s'", action, id));
						}
					}
					rules.clear();
					rules.addAll(out);
					rulesQTime = System.currentTimeMillis();
				}
			}
			catch (SQLException e)
			{
				LogHelper.error(e);
				throw new RuntimeException("Database error occurred, exiting...");
			}
		}
		return rules;
	}

	public String getUrl()
	{
		return url;
	}
	
	public boolean isActive()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return active;
	}

	public boolean isAdmActive()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return admActive;
	}
}
