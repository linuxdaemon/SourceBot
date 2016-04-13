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

import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.bot.search.SearchInclusion;
import net.walterbarnes.sourcebot.common.config.types.BlogConfig;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlogTerm extends SearchTerm
{
	private static final Logger logger = Logger.getLogger(BlogTerm.class.getName());
	private static final String type = "blog";

	public BlogTerm(String term, Tumblr client, BlogConfig blog)
	{
		super(term, client, blog);
	}

	@Override
	public PostCache getCache()
	{
		return cache;
	}

	@Override
	public String getSearchTerm()
	{
		return type + ":" + term;
	}

	@Override
	public Map<Post, String> getPosts(List<String> blogBlacklist, List<String> tagBlacklist, SearchInclusion rule) throws SQLException
	{
		List<Long> postBlacklist = blog.getPosts();

		int postNum = lastPostCount > 0 ? lastPostCount : (rule.getSampleSize() == 0 ? blog.getSampleSize() : rule.getSampleSize());
		String[] types = rule.getPostType() == null ? blog.getPostType() : rule.getPostType();

		lastTime = System.currentTimeMillis() / 1000;
		searched = 0;

		long start = System.currentTimeMillis();

		cache.validate();

		Map<Post, String> out = new HashMap<>();

		for (Post p : cache)
		{
			out.put(p, rule.getFullTerm());
		}

		logger.info(String.format("Searching %s %s", type, term));
		while (out.size() < postNum)
		{
			Map<String, Object> options = new HashMap<>();

			options.put("offset", searched);

			List<Post> posts;
			try
			{
				posts = client.blogPosts(term, options);
			}
			catch (JumblrException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				continue;
			}

			searched += posts.size();

			if (posts.size() == 0 || posts.isEmpty()) break;
			filterPosts(out, posts, postBlacklist, tagBlacklist, blogBlacklist, rule);
//			loop:
//			for (Post post : posts)
//			{
//				if (post.getTimestamp() < lastTime) lastTime = post.getTimestamp();
//				if (!Arrays.asList(types).contains(post.getType().getValue())) continue;
//
//				if (blogBlacklist.contains(post.getBlogName()) || postBlacklist.contains(post.getId())) continue;
//
//				if (rule.getRequiredTags() != null)
//				{
//					for (String rt : rule.getRequiredTags())
//					{
//						if (!post.getTags().contains(rt)) continue loop;
//					}
//				}
//
//				for (String tag : tagBlacklist)
//				{
//					if (rule.getRequiredTags() != null && Arrays.asList(rule.getRequiredTags()).contains(tag))
//					{
//						continue;
//					}
//					if (postContains(post, tag)) continue loop;
//					if (post.getTags().contains(tag))
//					{
//						if (rule.getRequiredTags() == null || !Arrays.asList(rule.getRequiredTags()).contains(tag))
//						{
//							continue loop;
//						}
//					}
//				}
//
//				if (cache.addPost(post)) out.put(post, rule.getFullTerm());
//			}
		}
		long end = System.currentTimeMillis() - start;

		logger.info(String.format("Searched %s %s, selected %d posts out of %d searched (%f%%), took %d ms", type, term,
				out.size(), searched, ((double) (((float) out.size()) / ((float) searched)) * 100), end));

		blog.addStat(rule.getType().toString(), term, (int) end, searched, out.size());
		lastPostCount = out.size();
		return out;
	}
}
