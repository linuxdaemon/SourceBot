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

import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.common.config.types.BlogConfig;
import net.walterbarnes.sourcebot.common.tumblr.BlogTerm;
import net.walterbarnes.sourcebot.common.tumblr.PostUtil;
import net.walterbarnes.sourcebot.common.tumblr.SearchTerm;
import net.walterbarnes.sourcebot.common.tumblr.TagTerm;

import java.util.*;

public class SearchInclusion extends SearchRule
{
	private final String[] requiredTags;
	private final String[] postType;
	private final String postSelect;
	private final int sampleSize;

	public SearchInclusion(BlogConfig blog, int id, String blogId, String type, String term, String[] requiredTags, String[] postType, String postSelect,
						   int sampleSize, boolean active, long modified)
	{
		super(blog, id, blogId, SearchType.getType(type), term, active, modified);
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

	public Map<Post, String> filterPosts(List<Post> posts, List<Long> postBlacklist, List<String> tagBlacklist, List<String> blogBlacklist, SearchTerm searchTerm)
	{
		Map<Post, String> out = new HashMap<>();
		String[] types = getPostType() == null ? getBlog().getPostType() : getPostType();
		loop:
		for (Post post : posts)
		{
			if (post.getTimestamp() < searchTerm.lastTime) searchTerm.lastTime = post.getTimestamp();
			if (!Arrays.asList(types).contains(post.getType().getValue())) continue;

			if (blogBlacklist.contains(post.getBlogName()) || postBlacklist.contains(post.getId())) continue;

			if (getRequiredTags() != null)
			{
				for (String rt : getRequiredTags())
				{
					if (!post.getTags().contains(rt)) continue loop;
				}
			}

			for (String tag : tagBlacklist)
			{
				if (getRequiredTags() != null && Arrays.asList(getRequiredTags()).contains(tag))
				{
					continue;
				}
				if (PostUtil.postContains(post, tag)) continue loop;
				if (post.getTags().contains(tag))
				{
					if (getRequiredTags() == null || !Arrays.asList(getRequiredTags()).contains(tag))
					{
						continue loop;
					}
				}
			}
			if (searchTerm.getCache().addPost(post)) out.put(post, getFullTerm());
		}
		return out;
	}

	public String[] getPostType()
	{
		return postType == null ? null : postType.clone();
	}

	public String[] getRequiredTags()
	{
		return requiredTags == null ? null : requiredTags.clone();
	}
}
