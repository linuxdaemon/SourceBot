package net.walterbarnes.sourcebot;

import com.tumblr.jumblr.JumblrClient;

public class Tumblr
{
	private final JumblrClient client;

	public Tumblr(String consumerKey, String consumerSecret, String token, String tokenSecret)
	{
		client = new JumblrClient (consumerKey, consumerSecret);
		client.setToken (token, tokenSecret);
	}

	public void getPostsFromTag(String tag)
	{
		getPostsFromTag (tag, null);
	}

	public void getPostsFromTag(String tag, String type)
	{
		getPostsFromTag (tag, type, 20);
	}

	private void getPostsFromTag (String tag, String type, int i)
	{

	}
}
