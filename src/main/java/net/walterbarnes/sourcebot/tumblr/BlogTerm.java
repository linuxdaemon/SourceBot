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

package net.walterbarnes.sourcebot.tumblr;

import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.BotThread;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlogTerm implements SearchTerm
{
	private final String b;
	private final Tumblr client;
	private final Logger logger;
	private BotThread.Blog blog;
	private PostCache cache = new PostCache(120 * 60 * 1000);
	private int lastPostCount = 0;

	public BlogTerm(String b, Tumblr client, BotThread.Blog blog, Logger logger)
	{
		this.b = b;
		this.client = client;
		this.blog = blog;
		this.logger = logger;
	}

	@SuppressWarnings ("Duplicates")
	@Override
	public Map<Post, String> getPosts(List<String> blogBlacklist, List<String> tagBlacklist, String[] requiredTags,
									  String[] postType, String postSelect, int sampleSize, boolean active) throws SQLException
	{
		List<Long> postBlacklist = blog.getPosts();

		int postNum = lastPostCount > 0 ? lastPostCount : (sampleSize == 0 ? blog.getSampleSize() : sampleSize);
		String[] type = postType == null ? blog.getPostType() : postType;

		int postCount = 0;
		int searched = 0;

		long start = System.currentTimeMillis();

		cache.validate();

		Map<Post, String> out = new HashMap<>();

		for (Object obj : cache)
		{
			if (obj instanceof Post)
			{
				Post p = (Post) obj;
				out.put(p, String.format("blog:%s", b));
			}
			else
			{
				throw new RuntimeException("Non-post object in post cache");
			}
		}

		logger.info("Searching blog " + b);

		while (out.size() < postNum)
		{
			HashMap<String, Object> options = new HashMap<>();

			options.put("offset", postCount);

			List<Post> posts;
			try
			{
				posts = client.blogPosts(b, options);
			}
			catch (JumblrException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				continue;
			}

			if (posts.size() == 0 || posts.isEmpty()) break;

			loop:
			for (Post post : posts)
			{
				searched++;
				if (Arrays.asList(type).contains(post.getType().getValue()))
				{
					if (blogBlacklist.contains(post.getBlogName()) || postBlacklist.contains(post.getId())) continue;
					for (String tag : tagBlacklist)
					{
						if (post.getTags().contains(tag)) continue loop;
					}

					if (requiredTags != null)
					{
						for (String rt : requiredTags)
						{
							if (!post.getTags().contains(rt)) continue loop;
						}
					}

					if (cache.addPost(post)) out.put(post, String.format("blog:%s", b));
				}
			}
		}
		long end = System.currentTimeMillis() - start;

		logger.info(String.format("Searched blog %s, selected %d posts out of %d searched (%f%%), took %d ms", b,
				out.size(), searched, ((double) (((float) out.size()) / ((float) searched)) * 100), end));

		lastPostCount = out.size();
		blog.addStat("blog", b, (int) end, searched, out.size());
		return out;
	}

	@Override
	public PostCache getCache()
	{
		return cache;
	}

	@Override
	public String getSearchTerm()
	{
		return "blog:" + b;
	}
}
