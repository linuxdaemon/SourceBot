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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.responses.PostDeserializer;
import com.tumblr.jumblr.types.AnswerPost;
import com.tumblr.jumblr.types.Post;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TumblrApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@SuppressWarnings ({"WeakerAccess", "unused"})
public class Tumblr extends JumblrClient
{
	private final Logger logger;
	private final String consumerKey;
	private final String consumerSecret;
	private final Token token;

	public Tumblr(String consumerKey, String consumerSecret, String token, String tokenSecret, Logger logger)
	{
		super(consumerKey, consumerSecret);
		this.logger = logger;
		setToken(token, tokenSecret);
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.token = new Token(token, tokenSecret);
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
		long offset = 0;
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

	public List<Post> blogQueuedPosts(String blogName, long offset)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("offset", offset);
		return blogDraftPosts(blogName, params);
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
	 * Retrives a blogs drafts
	 *
	 * @param blogName Blog to retrieve posts from
	 * @param before   Retrieve posts before this id
	 * @return A List of posts from the blogs drafts
	 */
	public List<Post> blogDraftPosts(String blogName, long before)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("before_id", before);
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

	@SuppressWarnings ("Duplicates")
	public List<String> getRawTaggedPosts(String tag)
	{
		OAuthService service = new ServiceBuilder().
				provider(TumblrApi.class).
				apiKey(consumerKey).apiSecret(consumerSecret).
				build();
		//List<Post> posts = client.blogPosts("thelinuxdemon");
		String path = "/tagged";
		Map<String, Object> map = new HashMap<>();
		map.put("api_key", consumerKey);
		map.put("tag", tag);
		OAuthRequest request = getRequestBuilder().constructGet(path, map);
		if (token != null)
		{
			service.signRequest(token, request);
		}
		Gson gsonBuilder = new Gson();
		//Gson gsonBuilder = new GsonBuilder().setPrettyPrinting().create();
		JsonParser parser = new JsonParser();
		Response response = request.send();

		System.out.println(gsonBuilder.toJson(parser.parse(response.getBody())
				.getAsJsonObject().get("response").getAsJsonObject().getAsJsonArray("posts").get(0)));
		Gson gson = new GsonBuilder().
				registerTypeAdapter(Post.class, new PostDeserializer()).
				create();
		String post = "{\"blog_name\":\"hecklinghyena\",\"id\":141574004892,\"post_url\":\"http://hecklinghyena.tumblr.com/post/141574004892/propression-oppression-noticing-and-suffering\",\"slug\":\"propression-oppression-noticing-and-suffering\",\"type\":\"text\",\"date\":\"2016-03-24 00:12:26 GMT\",\"timestamp\":1458778346,\"state\":\"published\",\"format\":\"html\",\"reblog_key\":\"YOSHbzbt\",\"tags\":[],\"short_url\":\"https://tmblr.co/ZhF1th23sT_2S\",\"summary\":\"Oppression: -Noticing and suffering from inequality and mistreatment your entire life almost every day -Not being able to stand...\",\"followed\":false,\"highlighted\":[],\"liked\":false,\"note_count\":22,\"body\":\"\\u003cp\\u003e\\u003ca href\\u003d\\\"http://propression.tumblr.com/post/141301268295/oppression-noticing-and-suffering-from\\\" class\\u003d\\\"tumblr_blog\\\"\\u003epropression\\u003c/a\\u003e:\\u003c/p\\u003e\\u003cblockquote\\u003e\\n\\u003cp\\u003e\\u003cb\\u003eOppression: \\u003cbr/\\u003e\\u003c/b\\u003e-Noticing and suffering from inequality and mistreatment your entire life almost every day\\u003cbr/\\u003e-Not being able to stand up against said inequality because you will get killed or imprisoned \\u003c/p\\u003e\\n\\u003cp\\u003e\\u003cb\\u003eNot oppression: \\u003c/b\\u003e\\u003cbr/\\u003e-Not noticing inequality \\n\\nand mistreatment \\n\\nunless a certain group tells you about it\\u003cbr/\\u003e-Being able to stand up against this non-existing inequality while getting paid  doing it\\u003c/p\\u003e\\n\\u003cp\\u003eBroken~Seal\\u003c/p\\u003e\\n\\u003c/blockquote\\u003e\\n\\u003cp\\u003eSource?\\u003c/p\\u003e\",\"reblog\":{\"tree_html\":\"\\u003cp\\u003e\\u003ca href\\u003d\\\"http://propression.tumblr.com/post/141301268295/oppression-noticing-and-suffering-from\\\" class\\u003d\\\"tumblr_blog\\\"\\u003epropression\\u003c/a\\u003e:\\u003c/p\\u003e\\n\\u003cblockquote\\u003e\\n\\u003cp\\u003e\\u003cb\\u003eOppression: \\u003cbr\\u003e\\u003c/b\\u003e-Noticing and suffering from inequality and mistreatment your entire life almost every day\\u003cbr\\u003e-Not being able to stand up against said inequality because you will get killed or imprisoned \\u003c/p\\u003e\\n\\u003cp\\u003e\\u003cb\\u003eNot oppression: \\u003c/b\\u003e\\u003cbr\\u003e-Not noticing inequality \\n\\nand mistreatment \\n\\nunless a certain group tells you about it\\u003cbr\\u003e-Being able to stand up against this non-existing inequality while getting paid  doing it\\u003c/p\\u003e\\n\\u003cp\\u003eBroken~Seal\\u003c/p\\u003e\\n\\u003c/blockquote\\u003e\",\"comment\":\"\\u003cp\\u003eSource?\\u003c/p\\u003e\"},\"trail\":[{\"blog\":{\"name\":\"propression\",\"active\":true,\"theme\":{\"avatar_shape\":\"square\",\"background_color\":\"#1b3539\",\"body_font\":\"Helvetica Neue\",\"header_bounds\":0,\"header_image\":\"https://secure.static.tumblr.com/36eb5ce8f98bbd4f7174b9055a3c7437/2cg9sut/418o1rhpn/tumblr_static_el55gt8vpog0ggkw8gs0c8w4s.jpg\",\"header_image_focused\":\"https://secure.static.tumblr.com/36eb5ce8f98bbd4f7174b9055a3c7437/2cg9sut/418o1rhpn/tumblr_static_el55gt8vpog0ggkw8gs0c8w4s.jpg\",\"header_image_scaled\":\"https://secure.static.tumblr.com/36eb5ce8f98bbd4f7174b9055a3c7437/2cg9sut/418o1rhpn/tumblr_static_el55gt8vpog0ggkw8gs0c8w4s.jpg\",\"header_stretch\":true,\"link_color\":\"#0e5a5d\",\"show_avatar\":true,\"show_description\":true,\"show_header_image\":true,\"show_title\":true,\"title_color\":\"#CCCCCC\",\"title_font\":\"Bookmania\",\"title_font_weight\":\"bold\"},\"share_likes\":false,\"share_following\":false},\"post\":{\"id\":\"141301268295\"},\"content_raw\":\"\\u003cp\\u003e\\u003cb\\u003eOppression: \\u003cbr\\u003e\\u003c/b\\u003e-Noticing and suffering from inequality and mistreatment your entire life almost every day\\u003cbr\\u003e-Not being able to stand up against said inequality because you will get killed or imprisoned \\u003c/p\\u003e\\n\\u003cp\\u003e\\u003cb\\u003eNot oppression: \\u003c/b\\u003e\\u003cbr\\u003e-Not noticing inequality \\n\\nand mistreatment \\n\\nunless a certain group tells you about it\\u003cbr\\u003e-Being able to stand up against this non-existing inequality while getting paid  doing it\\u003c/p\\u003e\\n\\u003cp\\u003eBroken~Seal\\u003c/p\\u003e\",\"content\":\"\\u003cp\\u003e\\u003cb\\u003eOppression: \\u003cbr /\\u003e\\u003c/b\\u003e-Noticing and suffering from inequality and mistreatment your entire life almost every day\\u003cbr /\\u003e-Not being able to stand up against said inequality because you will get killed or imprisoned \\u003c/p\\u003e\\n\\u003cp\\u003e\\u003cb\\u003eNot oppression: \\u003c/b\\u003e\\u003cbr /\\u003e-Not noticing inequality \\n\\nand mistreatment \\n\\nunless a certain group tells you about it\\u003cbr /\\u003e-Being able to stand up against this non-existing inequality while getting paid  doing it\\u003c/p\\u003e\\n\\u003cp\\u003eBroken~Seal\\u003c/p\\u003e\",\"is_root_item\":true},{\"blog\":{\"name\":\"hecklinghyena\",\"active\":true,\"theme\":{\"avatar_shape\":\"square\",\"background_color\":\"#FAFAFA\",\"body_font\":\"Helvetica Neue\",\"header_bounds\":0,\"header_image\":\"https://secure.assets.tumblr.com/images/default_header/optica_pattern_04.png?_v\\u003d7c4e5e82cf797042596e2e64af1c383f\",\"header_image_focused\":\"https://secure.assets.tumblr.com/images/default_header/optica_pattern_04.png?_v\\u003d7c4e5e82cf797042596e2e64af1c383f\",\"header_image_scaled\":\"https://secure.assets.tumblr.com/images/default_header/optica_pattern_04.png?_v\\u003d7c4e5e82cf797042596e2e64af1c383f\",\"header_stretch\":true,\"link_color\":\"#529ECC\",\"show_avatar\":true,\"show_description\":true,\"show_header_image\":true,\"show_title\":true,\"title_color\":\"#444444\",\"title_font\":\"Gibson\",\"title_font_weight\":\"bold\"},\"share_likes\":false,\"share_following\":false},\"post\":{\"id\":\"141574004892\"},\"content_raw\":\"\\u003cp\\u003eSource?\\u003c/p\\u003e\",\"content\":\"\\u003cp\\u003eSource?\\u003c/p\\u003e\",\"is_current_item\":true}],\"can_send_in_message\":true,\"can_reply\":false}";
		Post pst = gson.fromJson(post, new TypeToken<Post>() {}.getType());
		System.out.println(pst.getId());
		return null;
	}
}
