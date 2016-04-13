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

import net.walterbarnes.sourcebot.common.config.types.UserConfig;

import javax.annotation.CheckForNull;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConfig
{
	private static final Logger logger = Logger.getLogger(DBConfig.class.getName());
	private final String host;
	private final int port;
	private final String database;
	private final String user;
	private final String pass;
	private Driver driver;
	private String scheme;
	private Connection connection;

	public DBConfig(String host, int port, String database, String user, String pass)
	{
		this.host = host;
		this.port = port;
		this.database = database;
		this.user = user;
		this.pass = pass;
	}

	public DBConfig setDriver(String classPath) throws ClassNotFoundException, IllegalAccessException, InstantiationException
	{
		driver = (Driver) Class.forName(classPath).newInstance();
		return this;
	}

	public DBConfig setScheme(String scheme)
	{
		this.scheme = scheme;
		return this;
	}

	public void connect() throws SQLException
	{
		if (driver == null)
		{ throw new IllegalStateException("Driver and scheme must be configured before a connection can be made"); }
		connection = DriverManager.getConnection(String.format("%s://%s:%s/%s", scheme, host, port, database), user, pass);
	}

	@CheckForNull
	public UserConfig getUser(String name) throws SQLException
	{
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			st = connection.prepareStatement("SELECT * FROM users WHERE name = ?");
			st.setString(1, name);
			rs = st.executeQuery();
			boolean firstRun = true;
			UserConfig user = null;
			while (rs.next())
			{
				if (!firstRun)
				{ throw new IllegalStateException("Multiple users exist with name '" + name + "'"); }
				firstRun = false;
				user = new UserConfig(connection, rs.getString("id"), rs.getString("name"), rs.getString("email"),
						rs.getBoolean("is_email_verified"), rs.getInt("blog_allot"), rs.getBoolean("has_blog_limit"), rs.getBoolean("id_admin"));
			}
			return user;
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
	}
}
