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

import com.tumblr.jumblr.types.*;

public class PostUtil
{
	// Will look in to fixing the instanceof chain later, but with how the posts are done, I don't think it will be possible
	@SuppressWarnings ("ChainOfInstanceofChecks")
	public static boolean postContains(Post post, CharSequence s)
	{
		boolean found = false;
		if (post instanceof TextPost)
		{
			TextPost p = (TextPost) post;
			if ((p.getTitle() != null && p.getTitle().contains(s)) || (p.getBody() != null && p.getBody().contains(s)))
				found = true;
		}
		else if (post instanceof PhotoPost)
		{
			PhotoPost p = (PhotoPost) post;
			if (p.getCaption() != null && p.getCaption().contains(s)) found = true;
		}
		else if (post instanceof QuotePost)
		{
			QuotePost p = (QuotePost) post;
			if ((p.getSource() != null && p.getSource().contains(s)) || (p.getText() != null && p.getText().contains(s)))
				found = true;
		}
		else if (post instanceof LinkPost)
		{
			LinkPost p = (LinkPost) post;
			if ((p.getTitle() != null && p.getTitle().contains(s)) || (p.getDescription() != null && p.getDescription().contains(s)))
				found = true;
		}
		else if (post instanceof ChatPost)
		{
			ChatPost p = (ChatPost) post;
			if ((p.getTitle() != null && p.getTitle().contains(s)) || (p.getBody() != null && p.getBody().contains(s)))
				found = true;
			for (Dialogue line : p.getDialogue())
			{
				if (line.getPhrase().contains(s) || line.getLabel().contains(s) || line.getName().contains(s))
					found = true;
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
}
