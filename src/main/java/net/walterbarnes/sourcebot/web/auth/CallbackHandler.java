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

import net.walterbarnes.sourcebot.common.reference.Constants;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TumblrApi;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class CallbackHandler extends AbstractHandler
{
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		HttpSession session = baseRequest.getSession();
		response.setContentType("text/plain; charset=utf-8");

		String consumerKey = (String) session.getAttribute("consumerKey");
		String consumerSecret = (String) session.getAttribute("consumerSecret");
		OAuthService service = new ServiceBuilder()
				.provider(TumblrApi.class)
				.apiKey(consumerKey)
				.apiSecret(consumerSecret)
				//.debugStream(System.out)
				.build();

		Token token = new Token((String) session.getAttribute("requestToken"), (String) session.getAttribute("requestTokenSecret"));
		Verifier verifier = new Verifier(baseRequest.getParameterMap().get("oauth_verifier")[0]);

		try
		{
			Token accessToken = service.getAccessToken(token, verifier);
			session.removeAttribute("requestToken");
			session.removeAttribute("requestTokenSecret");

			session.setAttribute("consumerKey", consumerKey);
			session.setAttribute("consumerSecret", consumerSecret);
			session.setAttribute("token", accessToken.getToken());
			session.setAttribute("tokenSecret", accessToken.getSecret());
			if (session.getAttribute("lastPage") == null)
			{
				session.setAttribute("lastPage", "/");
			}
			response.sendRedirect(Constants.getWebRoot() + session.getAttribute("lastPage"));
			response.setStatus(HttpServletResponse.SC_OK);
		}
		catch (Exception e)
		{
			e.printStackTrace(response.getWriter());
		}
		baseRequest.setHandled(true);
	}
}


