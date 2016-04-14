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

package net.walterbarnes.sourcebot.common.tumblr;

import com.tumblr.jumblr.types.*;
import net.walterbarnes.sourcebot.bot.search.SearchInclusion;
import net.walterbarnes.sourcebot.bot.search.SearchRule;
import net.walterbarnes.sourcebot.common.config.types.BlogConfig;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public abstract class SearchTerm implements ISearchTerm
{
	private static final Logger logger = Logger.getLogger(SearchTerm.class.getName());
	final PostCache cache = new PostCache(120 * 60 * 1000);
	private final String term;
	private final SearchRule.SearchType type;
	long lastTime;
	int searched;
	int lastPostCount = 0;
	private Tumblr client;
	private BlogConfig blog;

	SearchTerm(String term, SearchRule.SearchType type)
	{
		this.term = term;
		this.type = type;
	}

	@Override
	public PostCache getCache()
	{
		return cache;
	}

	@Override
	public String getSearchTerm()
	{
		return getType() + ":" + getTerm();
	}

	@Override
	public Map<Post, String> getPosts(List<String> blogBlacklist, List<String> tagBlacklist, SearchInclusion rule) throws SQLException
	{
		List<Long> postBlacklist = getBlog().getPosts();

		int postNum = lastPostCount > 0 ? lastPostCount : (rule.getSampleSize() == 0 ? getBlog().getSampleSize() : rule.getSampleSize());

		lastTime = System.currentTimeMillis() / 1000;
		searched = 0;

		long start = System.currentTimeMillis();

		cache.validate();

		Map<Post, String> out = new HashMap<>();

		for (Post p : cache)
		{
			out.put(p, rule.getFullTerm());
		}

		logger.info(String.format("Searching %s %s", getType(), getTerm()));
		while (out.size() < postNum)
		{
			List<Post> posts = getPostSet();

			if (posts.size() == 0 || posts.isEmpty()) break;
			filterPosts(out, posts, postBlacklist, tagBlacklist, blogBlacklist, rule);
		}
		long end = System.currentTimeMillis() - start;

		logger.info(String.format("Searched %s %s, selected %d posts out of %d searched (%f%%), took %d ms", getType(), getTerm(),
				out.size(), searched, ((double) (((float) out.size()) / ((float) searched)) * 100), end));

		getBlog().addStat(rule.getType().toString(), getTerm(), (int) end, searched, out.size());
		lastPostCount = out.size();
		return out;
	}

	public BlogConfig getBlog()
	{
		return blog;
	}

	void setBlog(BlogConfig blog)
	{
		this.blog = blog;
	}

	public abstract List<Post> getPostSet();

	void filterPosts(Map<Post, String> out, List<Post> posts, List<Long> postBlacklist, List<String> tagBlacklist, List<String> blogBlacklist, SearchInclusion rule)
	{
		String[] types = rule.getPostType() == null ? blog.getPostType() : rule.getPostType();
		loop:
		for (Post post : posts)
		{
			if (post.getTimestamp() < lastTime) lastTime = post.getTimestamp();
			if (!Arrays.asList(types).contains(post.getType().getValue())) continue;

			if (blogBlacklist.contains(post.getBlogName()) || postBlacklist.contains(post.getId())) continue;

			if (rule.getRequiredTags() != null)
			{
				for (String rt : rule.getRequiredTags())
				{
					if (!post.getTags().contains(rt)) continue loop;
				}
			}

			for (String tag : tagBlacklist)
			{
				if (rule.getRequiredTags() != null && Arrays.asList(rule.getRequiredTags()).contains(tag))
				{
					continue;
				}
				if (postContains(post, tag)) continue loop;
				if (post.getTags().contains(tag))
				{
					if (rule.getRequiredTags() == null || !Arrays.asList(rule.getRequiredTags()).contains(tag))
					{
						continue loop;
					}
				}
			}
			if (cache.addPost(post)) out.put(post, rule.getFullTerm());
		}
	}

	private static boolean postContains(Post post, String s)
	{
		boolean found = false;
		if (post instanceof TextPost)
		{
			TextPost p = (TextPost) post;
			if (p.getTitle().contains(s) || p.getBody().contains(s)) found = true;
		}
		else if (post instanceof PhotoPost)
		{
			PhotoPost p = (PhotoPost) post;
			if (p.getCaption().contains(s)) found = true;
		}
		else if (post instanceof QuotePost)
		{
			QuotePost p = (QuotePost) post;
			if (p.getSource().contains(s) || p.getText().contains(s)) found = true;
		}
		else if (post instanceof LinkPost)
		{
			LinkPost p = (LinkPost) post;
			if (p.getTitle().contains(s) || p.getDescription().contains(s)) found = true;
		}
		else if (post instanceof ChatPost)
		{
			ChatPost p = (ChatPost) post;
			if (p.getTitle().contains(s) || p.getBody().contains(s)) found = true;
			for (Dialogue line : p.getDialogue())
			{
				if (line.getPhrase().contains(s) || line.getLabel().contains(s) || line.getName().contains(s))
				{ found = true; }
			}
		}
		else if (post instanceof AudioPost)
		{
			AudioPost p = (AudioPost) post;
			if (p.getCaption().contains(s)) found = true;
		}
		else if (post instanceof VideoPost)
		{
			VideoPost p = (VideoPost) post;
			if (p.getCaption().contains(s)) found = true;
		}
		else if (post instanceof AnswerPost)
		{
			AnswerPost p = (AnswerPost) post;
			if (p.getAnswer().contains(s) || p.getQuestion().contains(s)) found = true;
		}
		else if (post instanceof PostcardPost)
		{
			PostcardPost p = (PostcardPost) post;
			if (p.getBody().contains(s)) found = true;
		}
		return found;
	}

	public SearchRule.SearchType getType()
	{
		return type;
	}

	public String getTerm()
	{
		return term;
	}

	public Tumblr getClient()
	{
		return client;
	}

	void setClient(Tumblr client)
	{
		this.client = client;
	}
}
