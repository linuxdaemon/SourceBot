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

package net.walterbarnes.sourcebot.web.auth;

import com.tumblr.jumblr.exceptions.JumblrException;
import net.walterbarnes.sourcebot.tumblr.Tumblr;

import javax.servlet.http.HttpSession;

public class TumblrAuth
{
	private HttpSession session;
	private Tumblr client;

	public TumblrAuth(HttpSession session)
	{
		this.session = session;
	}

	public boolean authenticate()
	{
		if (session.getAttribute("loggedIn") != null && (boolean) session.getAttribute("loggedIn"))
		{
			if (sessionHas("consumerKey") && sessionHas("consumerSecret")
					&& sessionHas("token") && sessionHas("tokenSecret"))
			{
				try
				{
					String consumerKey = (String) session.getAttribute("consumerKey");
					String consumerSecret = (String) session.getAttribute("consumerSecret");
					String token = (String) session.getAttribute("token");
					String tokenSecret = (String) session.getAttribute("tokenSecret");
					Tumblr client = new Tumblr(consumerKey, consumerSecret, token, tokenSecret);
					return true;
				}
				catch (JumblrException e)
				{
					return false;
				}
			}
		}
		return false;
	}

	private boolean sessionHas(String name)
	{
		return session.getAttribute(name) != null;
	}

	public boolean isRegistered()
	{
		return true;
	}

	public Tumblr getClient()
	{
		if (this.client == null)
		{
			this.client = new Tumblr((String) session.getAttribute("consumerKey"), (String) session.getAttribute("consumerSecret"),
					(String) session.getAttribute("token"), (String) session.getAttribute("tokenSecret"));
		}
		return this.client;
	}

	public void logout()
	{
		session.invalidate();
	}
}
