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

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TumblrApi;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class ConnectHandler extends AbstractHandler
{
	private final String consumerKey;
	private final String consumerSecret;

	public ConnectHandler(String consumerKey, String consumerSecret)
	{
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		HttpSession session = baseRequest.getSession();
		OAuthService service = new ServiceBuilder()
				.provider(TumblrApi.class)
				.apiKey(consumerKey)
				.apiSecret(consumerSecret)
				.callback("http://localhost:8087/callback")
				//.debugStream(System.out)
				.build();

		Token token = service.getRequestToken();

		session.setAttribute("consumerKey", consumerKey);
		session.setAttribute("consumerSecret", consumerSecret);
		session.setAttribute("requestToken", token.getToken());
		session.setAttribute("requestTokenSecret", token.getSecret());

		response.sendRedirect(service.getAuthorizationUrl(token));
		baseRequest.setHandled(true);
	}
}
