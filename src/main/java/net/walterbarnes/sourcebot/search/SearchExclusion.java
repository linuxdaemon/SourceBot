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

package net.walterbarnes.sourcebot.search;

public class SearchExclusion implements ISearch
{
	private int id;
	private String type;
	private String term;
	private boolean active;

	public SearchExclusion(int id, String type, String term, boolean active)
	{
		this.id = id;
		this.type = type;
		this.term = term;
		this.active = active;
	}

	@Override
	public String getType()
	{
		return type;
	}

	@Override
	public String getTerm()
	{
		return term;
	}

	@Override
	public int getId()
	{
		return id;
	}

	@Override
	public boolean isActive()
	{
		return active;
	}
}
