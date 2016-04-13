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

import com.tumblr.jumblr.types.Blog;
import net.walterbarnes.sourcebot.common.tumblr.Tumblr;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UserConfig
{
	private static final Logger logger = Logger.getLogger(UserConfig.class.getName());
	private final Connection connection;
	private final String id;
	private final Tumblr client;
	private String name;
	private String email;
	private boolean isEmailVerified;
	private int blogAllot;
	private boolean hasBlogLimit;
	private boolean isAdmin;

	public UserConfig(Tumblr client, Connection connection, String id)
	{
		this.client = client;
		this.connection = connection;
		this.id = id;
	}

	public Connection getConnection()
	{
		return connection;
	}

	public String getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public String getEmail()
	{
		return email;
	}

	public boolean isEmailVerified()
	{
		return isEmailVerified;
	}

	public int getBlogAllot()
	{
		return blogAllot;
	}

	public boolean hasBlogLimit()
	{
		return hasBlogLimit;
	}

	public List<BlogConfig> getBlogs()
	{
		PreparedStatement st = null;
		ResultSet rs = null;
		List<BlogConfig> blogs = new ArrayList<>();
		try
		{
			List<String> urlArr = client.user().getBlogs().stream().map(Blog::getName).collect(Collectors.toList());

			st = connection.prepareStatement("SELECT id FROM blogs WHERE user_id = ? OR url = ANY(?) OR ?");
			st.setString(1, id);
			st.setString(2, "{" + StringUtils.join(urlArr, ",") + "}");
			st.setBoolean(3, isAdmin());
			rs = st.executeQuery();
			while (rs.next())
			{
				blogs.add(new BlogConfig(client, connection, rs.getString("id")));
			}
			return blogs;
		}
		catch (SQLException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
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
			if (st != null)
			{
				try
				{
					st.close();
				}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
		return blogs;
	}

	public boolean isAdmin()
	{
		return isAdmin;
	}
}
