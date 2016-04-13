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

import java.sql.Connection;

public class UserConfig
{
	private final Connection connection;
	private final String id;
	private final String name;
	private final String email;
	private final boolean isEmailVerified;
	private final int blogAllot;
	private final boolean hasBlogLimit;
	private final boolean idAdmin;

	public UserConfig(Connection connection, String id, String name, String email, boolean isEmailVerified, int blogAllot, boolean hasBlogLimit, boolean idAdmin)
	{
		this.connection = connection;
		this.id = id;
		this.name = name;
		this.email = email;
		this.isEmailVerified = isEmailVerified;
		this.blogAllot = blogAllot;
		this.hasBlogLimit = hasBlogLimit;
		this.idAdmin = idAdmin;
	}
}
