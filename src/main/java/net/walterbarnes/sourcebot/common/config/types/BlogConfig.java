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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings ("UnusedReturnValue")
public class BlogConfig
{
	private static final Logger logger = Logger.getLogger(BlogConfig.class.getName());

	private final PreparedStatement addPosts;
	private final PreparedStatement addStats;
	private final PreparedStatement getConfig;
	private final PreparedStatement getPosts;
	private final Tumblr client;
	private final String url;
	private final List<SearchRule> rules = new ArrayList<>();
	private final Connection connection;
	private final String id;
	private long rulesQTime = 0;
	private ResultSet configRs;
	private long configQTime = 0;
	
	public BlogConfig(@Nonnull Tumblr client, @Nonnull Connection connection, @Nonnull String id) throws SQLException
	{
		PreparedStatement getUrl = null;
		ResultSet rs = null;
		try
		{
			getUrl = connection.prepareStatement("SELECT url FROM blogs WHERE id = ?");
			getUrl.setString(1, id);
			rs = getUrl.executeQuery();
			rs.next();
			this.url = rs.getString("url");
			this.client = client;
			this.connection = connection;
			this.id = id;
			getConfig = connection.prepareStatement("SELECT * FROM blogs WHERE id = ?::UUID", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			getConfig.setString(1, id);

			getPosts = connection.prepareStatement("SELECT post_id FROM seen_posts WHERE blog_id = ?::UUID", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			getPosts.setString(1, id);

			addPosts = connection.prepareStatement("INSERT INTO seen_posts (blog_id, search_type, post_id, rb_id, search_term, blog) VALUES (?::UUID, ?, ?, ?, ?, ?)", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			addPosts.setString(1, id);

			addStats = connection.prepareStatement("INSERT INTO search_stats (blog_id, type, term, search_time, searched, selected) VALUES (?::UUID, ?, ?, ?, ?, ?)", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			addStats.setString(1, id);
		}
		finally
		{
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			if (getUrl != null)
			{
				try
				{
					getUrl.close();
				}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
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
		try
		{
			if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
			{
				configRs = getConfig.executeQuery();
				configQTime = System.currentTimeMillis();
			}
			configRs.beforeFirst();
			if (configRs.next())
			{ return configRs.getString("post_state"); }
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
		return null;
	}

	public int getPostBuffer()
	{
		try
		{
			if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
			{
				configRs = getConfig.executeQuery();
				configQTime = System.currentTimeMillis();
			}
			configRs.beforeFirst();
			if (configRs.next())
			{ return configRs.getInt("post_buffer"); }
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			return 0;
		}
		return 0;
	}

	public boolean addStat(String type, String tag, int time, int searched, int selected) throws SQLException
	{
		addStats.setString(2, type);
		addStats.setString(3, tag);
		addStats.setInt(4, time);
		addStats.setInt(5, searched);
		addStats.setInt(6, selected);
		return addStats.execute();
	}

	public boolean addPost(String type, long id, long rbId, String tag, String blogName) throws SQLException
	{
		addPosts.setString(2, type);
		addPosts.setLong(3, id);
		addPosts.setLong(4, rbId);
		addPosts.setString(5, tag);
		addPosts.setString(6, blogName);
		return addPosts.execute();
	}

	public List<Long> getPosts() throws SQLException
	{
		ResultSet rs = null;
		try
		{
			List<Long> out = new ArrayList<>();
			rs = getPosts.executeQuery();
			while (rs.next()) out.add(rs.getLong("post_id"));
			return out;
		}
		finally
		{
			if (rs != null)
			{
				try {rs.close();}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}

	public String[] getPostType()
	{
		try
		{
			if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
			{
				configRs = getConfig.executeQuery();
				configQTime = System.currentTimeMillis();
			}
			configRs.beforeFirst();
			if (configRs.next())
			{
				return (String[]) configRs.getArray("post_type").getArray();
			}
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			return new String[0];
		}
		return new String[0];
	}

	public boolean getCheckBlog()
	{
		try
		{
			if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
			{
				configRs = getConfig.executeQuery();
				configQTime = System.currentTimeMillis();
			}
			configRs.beforeFirst();
			if (configRs.next())
			{ return configRs.getBoolean("blog_check_active"); }
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			return false;
		}
		return false;
	}

	public boolean getPreserveTags()
	{
		try
		{
			if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
			{
				configRs = getConfig.executeQuery();
				configQTime = System.currentTimeMillis();
			}
			configRs.beforeFirst();
			if (configRs.next())
			{ return configRs.getBoolean("preserve_tags"); }
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			return false;
		}
		return false;
	}

	public String getPostSelect()
	{
		try
		{
			if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
			{
				configRs = getConfig.executeQuery();
				configQTime = System.currentTimeMillis();
			}
			configRs.beforeFirst();
			if (configRs.next())
			{ return configRs.getString("post_select"); }
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
		return null;
	}

	public String getPostComment()
	{
		try
		{
			if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
			{
				configRs = getConfig.executeQuery();
				configQTime = System.currentTimeMillis();
			}
			configRs.beforeFirst();
			if (configRs.next())
			{ return configRs.getString("post_comment"); }
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
		return null;
	}

	public String[] getPostTags()
	{
		try
		{
			if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
			{
				configRs = getConfig.executeQuery();
				configQTime = System.currentTimeMillis();
			}
			configRs.beforeFirst();
			if (configRs.next())
			{
				return (String[]) configRs.getArray("post_tags").getArray();
			}
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			return new String[0];
		}
		return new String[0];
	}

	public int getSampleSize()
	{
		try
		{
			if (System.currentTimeMillis() - configQTime > DB.getCacheTime())
			{
				configRs = getConfig.executeQuery();
				configQTime = System.currentTimeMillis();
			}
			configRs.beforeFirst();
			if (configRs.next())
			{ return configRs.getInt("sample_size"); }
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			return 0;
		}
		return 0;
	}

	public List<SearchRule> getSearchRules() throws SQLException
	{
		PreparedStatement getRules = null;
		ResultSet rs = null;
		try
		{
			if (System.currentTimeMillis() - rulesQTime > DB.getCacheTime())
			{
				List<SearchRule> out = new ArrayList<>();
				getRules = connection.prepareStatement("SELECT 'include' AS action,* FROM search_inclusions WHERE blog_id = ? UNION ALL SELECT 'exclude' AS action,id,blog_id,type,term,NULL,NULL,NULL,NULL,active,modified FROM search_exclusions WHERE blog_id = ? ORDER BY term");
				getRules.setString(1, id);
				getRules.setString(2, id);
				rs = getRules.executeQuery();
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
			return rules;
		}
		finally
		{
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			if (getRules != null)
			{
				try
				{
					getRules.close();
				}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}
}
