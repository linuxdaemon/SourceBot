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

import com.tumblr.jumblr.types.Blog;
import net.walterbarnes.sourcebot.bot.config.DB;
import net.walterbarnes.sourcebot.bot.tumblr.Tumblr;
import net.walterbarnes.sourcebot.bot.util.LogHelper;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserConfig
{
	private final Connection connection;
	private final String id;
	private final Tumblr client;
	private final List<BlogConfig> blogs = new ArrayList<>();
	private String name;
	private String email;
	private boolean isEmailVerified;
	private int blogAllot;
	private boolean hasBlogLimit;
	private boolean isAdmin;
	private long queryTime = 0;
	private long blogQTime = 0;

	public UserConfig(Tumblr client, Connection connection, String id)
	{
		this.client = client;
		this.connection = connection;
		this.id = id;
	}

	public String getId()
	{
		return id;
	}

	public String getName()
	{
		if (System.currentTimeMillis() - queryTime > DB.getCacheTime())
		{
			loadUserData();
		}
		return name;
	}

	private void loadUserData()
	{
		try (PreparedStatement getData = connection.prepareStatement("SELECT name,email,is_email_verified,blog_allot,has_blog_limit,is_admin FROM users WHERE id = ?"))
		{
			getData.setString(1, id);
			try (ResultSet resultSet = getData.executeQuery())
			{
				boolean firstRun = true;
				while (resultSet.next())
				{
					if (!firstRun)
						throw new RuntimeException("Database error occurred, multiple users exist with uid '" + id + "'");
					firstRun = false;
					name = resultSet.getString("name");
					email = resultSet.getString("email");
					isEmailVerified = resultSet.getBoolean("is_email_verified");
					blogAllot = resultSet.getInt("blog_allot");
					hasBlogLimit = resultSet.getBoolean("has_blog_limit");
					isAdmin = resultSet.getBoolean("is_admin");
				}
				queryTime = System.currentTimeMillis();
			}
		}
		catch (SQLException e)
		{
			LogHelper.error(e);
			throw new RuntimeException("Database error occurred, exiting...");
		}
	}

	public String getEmail()
	{
		if (System.currentTimeMillis() - queryTime > DB.getCacheTime())
		{
			loadUserData();
		}
		return email;
	}

	public boolean isEmailVerified()
	{
		if (System.currentTimeMillis() - queryTime > DB.getCacheTime())
		{
			loadUserData();
		}
		return isEmailVerified;
	}

	public int getBlogAllot()
	{
		if (System.currentTimeMillis() - queryTime > DB.getCacheTime())
		{
			loadUserData();
		}
		return blogAllot;
	}

	public boolean hasBlogLimit()
	{
		if (System.currentTimeMillis() - queryTime > DB.getCacheTime())
		{
			loadUserData();
		}
		return hasBlogLimit;
	}

	public boolean isAdmin()
	{
		if (System.currentTimeMillis() - queryTime > DB.getCacheTime())
		{
			loadUserData();
		}
		return isAdmin;
	}

	public List<BlogConfig> getBlogs()
	{
		if (System.currentTimeMillis() - blogQTime > DB.getCacheTime())
		{
			try (PreparedStatement st = connection.prepareStatement("SELECT id FROM blogs WHERE user_id = ? OR url = ANY(?) OR ?"))
			{
				List<String> urlArr = client.user().getBlogs().stream().map(Blog::getName).collect(Collectors.toList());
				st.setString(1, id);
				st.setString(2, "{" + StringUtils.join(urlArr, ",") + "}");
				st.setBoolean(3, isAdmin);
				try (ResultSet rs = st.executeQuery())
				{
					blogs.clear();
					while (rs.next())
					{
						blogs.add(new BlogConfig(client, connection, rs.getString("id")));
					}
					blogQTime = System.currentTimeMillis();
				}
			}
			catch (SQLException e)
			{
				LogHelper.error(e);
				throw new RuntimeException("Database error occurred, exiting...");
			}
		}
		return blogs;
	}
}
