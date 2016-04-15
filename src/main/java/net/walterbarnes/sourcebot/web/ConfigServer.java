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

package net.walterbarnes.sourcebot.web;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigServer extends Server
{
	private final Map<String, AbstractHandler> pages = new HashMap<>();

	public ConfigServer(int port)
	{
		super(port);
		// Specify the Session ID Manager
		SessionIdManager idManager = new HashSessionIdManager();
		setSessionIdManager(idManager);

		// Sessions are bound to a context.
		ContextHandler context = new ContextHandler("/");
		setHandler(context);

		// Create the SessionHandler (wrapper) to handle the sessions
		HashSessionManager manager = new HashSessionManager();
		SessionHandler sessions = new SessionHandler(manager);
		context.setHandler(sessions);
		sessions.setHandler(new Handler());
		//context.setWelcomeFiles();
	}

	public void addPage(String path, AbstractHandler page)
	{
		pages.put(path, page);
	}

	private class Handler extends AbstractHandler
	{
		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
		{
			if (pages.containsKey(target))
			{
				pages.get(target).handle(target, baseRequest, request, response);
			}
		}
	}
}
