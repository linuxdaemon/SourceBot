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

import net.walterbarnes.sourcebot.search.SearchExclusion;
import net.walterbarnes.sourcebot.search.SearchInclusion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlogConfig
{
	private static final Logger logger = Logger.getLogger(BlogConfig.class.getName());
	private final PreparedStatement getBId;
	private final String id;
	private final PreparedStatement addPosts;
	private final PreparedStatement addStats;
	private final PreparedStatement getConfig;
	private final PreparedStatement getSI;
	private final PreparedStatement getSE;
	private final PreparedStatement getPosts;
	private final Connection conn;
	private final String url;
	private List<SearchExclusion> exclusions = new ArrayList<>();
	private long exclusionsQTime = 0;
	private List<SearchInclusion> inclusions = new ArrayList<>();
	private long inclusionsQTime = 0;
	private ResultSet configRs;
	private long configQTime = 0;

	public BlogConfig(Connection conn, String url) throws SQLException
	{
		this.conn = conn;
		this.url = url;
		getBId = conn.prepareStatement("SELECT id FROM blogs WHERE url = ?");
		getBId.setString(1, url);
		ResultSet rs = getBId.executeQuery();
		rs.next();
		id = rs.getString("id");

		getConfig = conn.prepareStatement("SELECT * FROM blogs WHERE id = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		getConfig.setString(1, id);

		getSI = conn.prepareStatement("SELECT DISTINCT * FROM search_inclusions WHERE blog_id = ? ORDER BY id", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		getSI.setString(1, id);

		getSE = conn.prepareStatement("SELECT DISTINCT * FROM search_exclusions WHERE blog_id = ? ORDER BY id", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		getSE.setString(1, id);

		getPosts = conn.prepareStatement("SELECT post_id FROM seen_posts WHERE blog_id = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		getPosts.setString(1, id);

		addPosts = conn.prepareStatement("INSERT INTO seen_posts (blog_id, search_type, post_id, rb_id, search_term, blog) VALUES (?, ?, ?, ?, ?, ?)", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		addPosts.setString(1, id);

		addStats = conn.prepareStatement("INSERT INTO search_stats (blog_id, type, term, search_time, searched, selected) VALUES (?, ?, ?, ?, ?, ?)", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		addStats.setString(1, id);
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
		List<Long> out = new ArrayList<>();
		ResultSet rs = getPosts.executeQuery();
		while (rs.next()) out.add(rs.getLong("post_id"));
		return out;
	}

	public String[] getPostType()
	{
		try
		{
			if (System.currentTimeMillis() - configQTime > 60000)
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
			return null;
		}
		return null;
	}

	public boolean getCheckBlog()
	{
		try
		{
			if (System.currentTimeMillis() - configQTime > 60000)
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
			if (System.currentTimeMillis() - configQTime > 60000)
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
			if (System.currentTimeMillis() - configQTime > 60000)
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

	public String getPostState()
	{
		try
		{
			if (System.currentTimeMillis() - configQTime > 60000)
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

	public String getPostComment()
	{
		try
		{
			if (System.currentTimeMillis() - configQTime > 60000)
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
			if (System.currentTimeMillis() - configQTime > 60000)
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
			return null;
		}
		return null;
	}

	public int getSampleSize()
	{
		try
		{
			if (System.currentTimeMillis() - configQTime > 60000)
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

	public int getPostBuffer()
	{
		try
		{
			if (System.currentTimeMillis() - configQTime > 60000)
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

	public List<SearchExclusion> getExclusions() throws SQLException
	{
		if (System.currentTimeMillis() - exclusionsQTime > 60000)
		{
			ResultSet rs = getSE.executeQuery();
			List<SearchExclusion> out = new ArrayList<>();
			while (rs.next())
			{
				out.add(new SearchExclusion(rs.getInt("id"), rs.getString("type"), rs.getString("term"),
						rs.getBoolean("active")));
			}
			exclusionsQTime = System.currentTimeMillis();
			return (exclusions = out);
		}
		return exclusions;
	}

	public List<SearchInclusion> getInclusions() throws SQLException
	{
		if (System.currentTimeMillis() - inclusionsQTime > 60000)
		{
			ResultSet rs = getSI.executeQuery();
			List<SearchInclusion> out = new ArrayList<>();
			while (rs.next())
			{
				int incId = rs.getInt("id");
				String type = rs.getString("type");
				String term = rs.getString("term");
				String[] required_tags = rs.getArray("required_tags") != null ? (String[]) rs.getArray("required_tags").getArray() : null;
				String[] postType = rs.getArray("post_type") != null ? (String[]) rs.getArray("post_type").getArray() : null;
				String postSelect = rs.getString("post_select");
				int sample = rs.getInt("sample_size");
				boolean active = rs.getBoolean("active");
				out.add(new SearchInclusion(incId, type, term, required_tags, postType, postSelect, sample, active));
			}
			inclusionsQTime = System.currentTimeMillis();
			return (inclusions = out);
		}
		return inclusions;
	}
}
