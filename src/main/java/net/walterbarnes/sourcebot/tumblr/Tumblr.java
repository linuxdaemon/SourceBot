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

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.AnswerPost;
import com.tumblr.jumblr.types.Post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@SuppressWarnings ({"WeakerAccess", "unused"})
public class Tumblr extends JumblrClient
{
	private static final Logger logger = Logger.getLogger(Tumblr.class.getName());

	public Tumblr(String consumerKey, String consumerSecret, String token, String tokenSecret)
	{
		super(consumerKey, consumerSecret);
		setToken(token, tokenSecret);
	}

	public List<AnswerPost> getAsks(String blogName)
	{
		int offset = 0;
		List<AnswerPost> asks = new ArrayList<>();
		List<Post> subs;
		while ((subs = blogSubmissions(blogName, offset)).size() > 0)
		{
			for (Post post : subs)
			{
				offset++;
				if (post.getType().getValue().equals("answer")) asks.add((AnswerPost) post);
			}
		}
		return asks;
	}

	public List<Post> blogSubmissions(String blogName, int offset)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("offset", offset);
		return blogSubmissions(blogName, params);
	}

	public List<Post> getQueuedPosts(String blogName)
	{
		int offset = 0;
		List<Post> queue;
		ArrayList<Post> out = new ArrayList<>();
		while ((queue = blogQueuedPosts(blogName, offset)).size() > 0)
			for (Post post : queue)
			{
				out.add(post);
				offset++;
			}
		return out;
	}

	public List<Post> blogQueuedPosts(String blogName, int offset)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("offset", String.valueOf(offset));
		return blogQueuedPosts(blogName, params);
	}

	public List<Post> getDrafts(String blogName)
	{
		long before = 0;
		List<Post> drafts;
		ArrayList<Post> out = new ArrayList<>();
		while ((drafts = blogDraftPosts(blogName, before)).size() > 0)
			for (Post post : drafts)
			{
				out.add(post);
				before = post.getId();
			}
		return out;
	}

	/**
	 * Retrieves a blogs drafts id {@code id}
	 *
	 * @param blogName Blog to retrieve posts from
	 * @param id   Retrieve posts before this id
	 * @return A List of posts from the blogs drafts
	 */
	public List<Post> blogDraftPosts(String blogName, long id)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("before_id", id);
		return blogDraftPosts(blogName, params);
	}

	public List<Post> getBlogPosts(String blogName)
	{
		List<Post> posts;
		ArrayList<Post> out = new ArrayList<>();
		while ((posts = blogPosts(blogName, out.size())).size() > 0)
			for (Post post : posts)
			{
				out.add(post);
			}
		return out;
	}

	public List<Post> blogPosts(String blogName, long before)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("offset", before);
		return blogPosts(blogName, params);
	}
}
