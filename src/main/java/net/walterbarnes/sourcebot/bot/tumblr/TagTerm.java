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

import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.bot.search.SearchRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TagTerm extends SearchTerm
{
	private static final Logger logger = Logger.getLogger(TagTerm.class.getName());

	public TagTerm(String term)
	{
		super(term, SearchRule.SearchType.TAG);
	}

	@Override
	public List<Post> getPostSet()
	{
		Map<String, Object> options = new HashMap<>();

		options.put("before", lastTime);
		List<Post> posts = new ArrayList<>();
		try
		{
			posts.addAll(getClient().tagged(getTerm(), options));
		}
		catch (JumblrException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		searched += posts.size();
		posts.stream().forEach(post -> {
			if (post.getTimestamp() > lastTime)
			{
				lastTime = post.getTimestamp();
			}
		});
		return posts;
	}
}
