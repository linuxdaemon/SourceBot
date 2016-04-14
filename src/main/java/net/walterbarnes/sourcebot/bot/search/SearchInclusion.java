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

import net.walterbarnes.sourcebot.common.tumblr.BlogTerm;
import net.walterbarnes.sourcebot.common.tumblr.SearchTerm;
import net.walterbarnes.sourcebot.common.tumblr.TagTerm;

import java.util.Optional;

public class SearchInclusion extends SearchRule
{
	private final String[] requiredTags;
	private final String[] postType;
	private final String postSelect;
	private final int sampleSize;

	public SearchInclusion(int id, String blogId, String type, String term, String[] requiredTags, String[] postType, String postSelect,
						   int sampleSize, boolean active, long modified)
	{
		super(id, blogId, SearchType.getType(type), term, active, modified);
		this.requiredTags = requiredTags == null ? null : requiredTags.clone();
		this.postType = postType == null ? null : postType.clone();
		this.postSelect = postSelect;
		this.sampleSize = sampleSize;
	}

	@Override
	public RuleAction getAction()
	{
		return RuleAction.INCLUDE;
	}

	public String[] getRequiredTags()
	{
		return requiredTags == null ? null : requiredTags.clone();
	}

	public String[] getPostType()
	{
		return postType == null ? null : postType.clone();
	}

	public String getPostSelect()
	{
		return postSelect;
	}

	public int getSampleSize()
	{
		return sampleSize;
	}

	public Optional<SearchTerm> getSearchTerm()
	{
		if (getType() == SearchType.TAG)
		{
			return Optional.of(new TagTerm(getTerm()));
		}
		else if (getType() == SearchType.BLOG)
		{
			return Optional.of(new BlogTerm(getTerm()));
		}
		return Optional.empty();
	}
}
