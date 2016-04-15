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

package net.walterbarnes.sourcebot.common.config.types;

import net.walterbarnes.sourcebot.bot.search.SearchExclusion;
import net.walterbarnes.sourcebot.bot.search.SearchInclusion;
import net.walterbarnes.sourcebot.bot.search.SearchRule;
import net.walterbarnes.sourcebot.common.config.DB;
import net.walterbarnes.sourcebot.common.tumblr.Tumblr;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings ("UnusedReturnValue")
public class BlogConfig
{
	private static final Logger logger = Logger.getLogger(BlogConfig.class.getName());

	private final Tumblr client;
	private final String url;
	private final Collection<SearchRule> rules = new ArrayList<>();
	private final Connection connection;
	private final String id;
	private long rulesQTime = 0;
	private long configQTime = 0;
	private Map<String, Object> config = new HashMap<>();
	private boolean active;
	private boolean admActive;

	public BlogConfig(@Nonnull Tumblr client, @Nonnull Connection connection, @Nonnull String id) throws SQLException
	{
		this.id = id;
		this.client = client;
		this.connection = connection;

		try (PreparedStatement getUrl = connection.prepareStatement("SELECT url FROM blogs WHERE id = ?"))
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
			logger.warning("Bot is not admin on '" + url + "', not running thread");
			return true;
		}
		return (getPostState().equals("draft") && client.getDrafts(url).size() < getPostBuffer()) ||
				(getPostState().equals("queue") && client.getQueuedPosts(url).size() < getPostBuffer());
	}

	public String getPostState()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return String.valueOf(config.get("post_state"));
	}

	private int getPostBuffer()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return (int) config.get("post_buffer");
	}

	private void loadConfig()
	{
		try (PreparedStatement st = connection.prepareStatement("SELECT * FROM blogs WHERE id = ?"))
		{
			st.setString(1, id);
			try (ResultSet rs = st.executeQuery())
			{
				boolean firstRun = true;
				while (rs.next())
				{
					if (!firstRun)
						throw new RuntimeException("Multiple blogs exists with id '" + id + "'");
					firstRun = false;
					config.clear();
					config.put("url", rs.getString("url"));
					config.put("blog_check_active", rs.getBoolean("blog_check_active"));
					config.put("sample_size", rs.getInt("sample_size"));
					config.put("post_type", rs.getArray("post_type"));
					config.put("post_select", rs.getString("post_select"));
					config.put("post_state", rs.getString("post_state"));
					config.put("post_buffer", rs.getInt("post_buffer"));
					config.put("post_comment", rs.getString("post_comment"));
					config.put("post_tags", rs.getArray("post_tags"));
					config.put("preserve_tags", rs.getBoolean("preserve_tags"));
					config.put("active", rs.getBoolean("active"));
					config.put("adm_active", rs.getBoolean("adm_active"));
				}
				configQTime = System.currentTimeMillis();
			}
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
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
			logger.log(Level.SEVERE, e.getMessage(), e);
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
			logger.log(Level.SEVERE, e.getMessage(), e);
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
			logger.log(Level.SEVERE, e.getMessage(), e);
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
		try
		{
			return (String[]) ((Array) config.get("post_type")).getArray();
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			return new String[0];
		}
	}

	public boolean getCheckBlog()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return (boolean) config.get("blog_check_active");
	}

	public boolean getPreserveTags()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return (boolean) config.get("preserve_tags");
	}

	public String getPostSelect()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return String.valueOf(config.get("post_select"));
	}

	public String getPostComment()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return String.valueOf(config.get("post_comment"));
	}

	public String[] getPostTags()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		try
		{
			return (String[]) ((Array) config.get("post_tags")).getArray();
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			return new String[0];
		}
	}

	public int getSampleSize()
	{
		if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
		{
			loadConfig();
		}
		return (int) config.get("sample_size");
	}

	public Collection<SearchRule> getSearchRules()
	{
		if (System.currentTimeMillis() - rulesQTime > DB.getCacheTime())
		{
			Collection<SearchRule> out = new ArrayList<>();
			try (PreparedStatement getRules = connection.prepareStatement("SELECT 'include' AS action,* FROM search_inclusions WHERE blog_id = ? UNION ALL SELECT 'exclude' AS action,id,blog_id,type,term,NULL,NULL,NULL,NULL,active,modified FROM search_exclusions WHERE blog_id = ? ORDER BY term"))
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
				logger.log(Level.SEVERE, e.getMessage(), e);
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
		return active;
	}

	public boolean isAdmActive()
	{
		return admActive;
	}
}
