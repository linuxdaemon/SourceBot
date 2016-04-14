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

package net.walterbarnes.sourcebot.bot.search;

public class SearchRule implements ISearch
{
	private final int id;
	private final SearchType type;
	private final String term;
	private final boolean active;

	SearchRule(int id, SearchType type, String term, boolean active)
	{
		this.id = id;
		this.type = type;
		this.term = term;
		this.active = active;
	}

	@Override
	public RuleAction getAction()
	{
		return RuleAction.UNKNOWN;
	}

	@Override
	public SearchType getType()
	{
		return type;
	}

	@Override
	public String getTerm()
	{
		return term;
	}

	@Override
	public String getFullTerm()
	{
		return String.format("%s:%s", this.type, this.term);
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

	public enum SearchType
	{
		TAG("tag"),
		BLOG("blog"),
		UNKNOWN("unknown");

		private final String name;

		SearchType(String name)
		{
			this.name = name;
		}

		public static SearchType getType(String name)
		{
			for (SearchType type : SearchType.values())
			{
				if (type.getName().equals(name))
				{
					return type;
				}
			}
			return UNKNOWN;
		}

		public String getName()
		{
			return name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum RuleAction
	{
		INCLUDE,
		EXCLUDE,
		UNKNOWN
	}
}
