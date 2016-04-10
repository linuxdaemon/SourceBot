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

package net.walterbarnes.sourcebot.command;

import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.SourceBot;

import java.util.List;
import java.util.logging.Logger;

public class CommandPurge implements ICommand
{
	private static final Logger logger = Logger.getLogger(CommandPurge.class.getName());

	@Override
	public void run(String... args)
	{
		List<Post> posts = null;
		logger.info(args[0]);
		switch (SourceBot.getCurrentBot().threads.get(args[0]).blog.getPostState())
		{
			case "draft":
				posts = SourceBot.getCurrentBot().client.getDrafts(args[0]);
				break;
			case "queue":
				posts = SourceBot.getCurrentBot().client.getQueuedPosts(args[0]);
				break;
		}
		if (posts != null)
		{
			for (Post post : posts)
			{
				post.delete();
			}
		}
	}
}
