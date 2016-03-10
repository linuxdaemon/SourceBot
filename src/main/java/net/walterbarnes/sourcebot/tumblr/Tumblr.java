package net.walterbarnes.sourcebot.tumblr;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.AnswerPost;
import com.tumblr.jumblr.types.Blog;
import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Tumblr extends JumblrClient
{
	private final Logger logger;
	private String blogName;

	public Tumblr(String consumerKey, String consumerSecret, String token, String tokenSecret, Logger logger)
	{
		super (consumerKey, consumerSecret);
		this.logger = logger;
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
		return getPostsFromTag(tag, type, postNum, opts, new ArrayList<String>(), new ArrayList<String>(),
				new ArrayList<Long>());
	}

	// TODO Implement Post Caching
	public ArrayList<Post> getPostsFromTag(String tag, String type, int postNum, HashMap<String, Object> opts,
										   List<String> blogBlacklist, List<String> tagBlacklist,
										   List<Long> postBlacklist)
	{
		int postCount = 0;
		long lastTime = System.currentTimeMillis () / 1000;
		ArrayList<Post> out = new ArrayList<> ();
		logger.info("Searching tag " + tag);
		System.out.print("Searching tag " + tag + " posts: " + postCount);
		while (postCount < postNum)
		{
			System.out.print("\r" + "Searching tag " + tag + " posts: " + postCount);
			HashMap<String, Object> options = new HashMap<> ();
			options.put ("before", lastTime);
			//options.put ("limit", 1);
			if (opts != null)
			{
				options.putAll (opts);
			}
			List<Post> posts = tagged (tag, options);
			if (posts.size () == 0 || posts.isEmpty ())
			{
				break;
			}
			loop:
			for (Post post : posts)
			{
				lastTime = post.getTimestamp();
				if (type == null || post.getType ().equals (type))
				{
					if (blogBlacklist.contains(post.getBlogName()) || postBlacklist.contains(post.getId())) continue;
					for (String t : tagBlacklist)
					{
						if (post.getTags().contains(t)) continue loop;
					}
					out.add (post);
					postCount++;
				}
			}
		}
		System.out.println();
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

	public List<Post> blogDraftPosts()
	{
		return blogDraftPosts(blogName);
	}

	public List<Post> blogSubmissions()
	{
		return blogSubmissions(blogName);
	}

	public List<Post> blogSubmissions(int offset)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("offset", offset);
		return blogSubmissions(blogName, params);
	}

	public List<AnswerPost> getAsks()
	{
		int offset = 0;
		List<AnswerPost> asks = new ArrayList<>();
		List<Post> subs;
		while ((subs = blogSubmissions(offset)).size() > 0)
		{
			for (Post post : subs)
			{
				offset++;
				if (post.getType().equals("answer"))
				{
					asks.add((AnswerPost)post);
				}
			}
		}
		return asks;
	}
}
