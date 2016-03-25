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
	private PostCache cache = new PostCache(30 * 60 * 1000);
	private int lastPostCount = 0;

	public BlogTerm(String b, Tumblr client, Logger logger)
	{
		this.b = b;
		this.client = client;
		this.logger = logger;
	}

	@Override
	public Map<Post, String> getPosts(Map<String, Object> opts, BotThread.Blog blog) throws SQLException
	{
		List<String> blogBlacklist = blog.getBlogBlacklist();
		List<String> tagBlacklist = blog.getTagBlacklist();
		List<Long> postBlacklist = blog.getPosts();

		int postNum = lastPostCount > 0 ? lastPostCount : blog.getSampleSize();
		String type = blog.getPostType();

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

			if (opts != null) options.putAll(opts);

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
				if (type == null || post.getType().getValue().equals(type))
				{
					for (String t : tagBlacklist) if (post.getTags().contains(t)) continue loop;

					if (postBlacklist.contains(post.getId())) continue;
					cache.addPost(post);
					out.put(post, String.format("blog:%s", b));
					postCount++;
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
}
