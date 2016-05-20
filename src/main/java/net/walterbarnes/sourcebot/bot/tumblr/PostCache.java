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

package net.walterbarnes.sourcebot.bot.tumblr;

import com.tumblr.jumblr.types.Post;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class PostCache implements Iterable<Post>
{
	private final Map<Post, Long> posts = new LinkedHashMap<>();
	private final long cacheLife;

	public PostCache(long cacheLife)
	{
		this.cacheLife = cacheLife;
	}

	public boolean addPost(Post post)
	{
		if (posts.containsKey(post)) return false;
		posts.put(post, System.currentTimeMillis());
		return true;
	}

	@SuppressWarnings ("UnusedReturnValue")
	public boolean remove(long postId)
	{
		for (Post p : posts.keySet())
		{
			if (p.getId().equals(postId))
			{
				posts.remove(p);
				return true;
			}
		}
		return false;
	}

	public void validate()
	{
		for (Post key : posts.keySet())
		{
			if (System.currentTimeMillis() - posts.get(key) < cacheLife)
			{
				posts.remove(key);
			}
		}
	}

	@SuppressWarnings ("unused")
	public int size()
	{
		return posts.size();
	}

	@Override
	public Iterator<Post> iterator()
	{
		return posts.keySet().iterator();
	}
}
