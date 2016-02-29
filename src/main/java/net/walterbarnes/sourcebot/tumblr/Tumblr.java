package net.walterbarnes.sourcebot.tumblr;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.Blog;
import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Tumblr extends JumblrClient
{
	private String blogName;

	public Tumblr (String consumerKey, String consumerSecret, String token, String tokenSecret)
	{
		super (consumerKey, consumerSecret);
		setToken (token, tokenSecret);
	}

	public ArrayList<Post> getPostsFromTag (String tag)
	{
		return getPostsFromTag (tag, null);
	}

	public ArrayList<Post> getPostsFromTag (String tag, String type)
	{
		return getPostsFromTag (tag, type, 20, null);
	}

	public ArrayList<Post> getPostsFromTag (String tag, String type, int postNum, HashMap<String, Object> opts)
	{
		int postCount = 0;
		long lastTime = System.currentTimeMillis () / 1000;
		ArrayList<Post> out = new ArrayList<> ();
		while (postCount < postNum)
		{
			HashMap<String, Object> options = new HashMap<> ();
			options.put ("before", lastTime);
			if (opts != null)
			{
				options.putAll (opts);
			}
			List<Post> posts = tagged (tag, options);
			if (posts.size () == 0 || posts.isEmpty ())
			{
				break;
			}
			for (Post post : posts)
			{
				if (type == null || post.getType ().equals (type))
				{
					out.add (post);
					postCount++;
				}
				lastTime = post.getTimestamp ();
			}
		}
		return out;
	}

	public JumblrClient getClient ()
	{
		return this;
	}

	public String getBlogName ()
	{
		return blogName;
	}

	public void setBlogName (String blog) throws InvalidBlogNameException
	{
		boolean valid = false;

		for (Blog b : user ().getBlogs ())
		{
			if (b.getName ().equals (blog))
			{
				valid = true;
			}
		}
		if (!valid)
		{
			throw new InvalidBlogNameException (blog);
		}
		this.blogName = blog;
	}
}
