package net.walterbarnes.sourcebot.tumblr;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.AnswerPost;
import com.tumblr.jumblr.types.Blog;
import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;

import java.sql.Connection;
import java.sql.SQLException;
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
		super(consumerKey, consumerSecret);
		this.logger = logger;
		setToken(token, tokenSecret);
	}

	// TODO Implement Post Caching
	public Map<Post, String> getPostsFromTag(String tag, String type, int postNum, HashMap<String, Object> opts,
											 List<String> blogBlacklist, List<String> tagBlacklist,
											 List<Long> postBlacklist, boolean checkBlog, Connection conn) throws SQLException
	{
		//PreparedStatement getPost = conn.prepareStatement("SELECT * FROM post_cache WHERE tag = ? AND time > NOW() - INTERVAL 1 DAY;");
		//getPost.setString(1, tag);
		//PreparedStatement addPost = conn.prepareStatement("INSERT INTO post_cache");
		int postCount = 0;
		int searched = 0;
		long lastTime = System.currentTimeMillis() / 1000;
		long start = System.currentTimeMillis();
		Map<Post, String> out = new HashMap<>();
		logger.info("Searching tag " + tag);
		//System.out.print("Searching tag " + tag + " posts: " + postCount);
		while (postCount < postNum)
		{
			//System.out.print("\r" + "Searching tag " + tag + " posts: " + postCount);
			HashMap<String, Object> options = new HashMap<>();
			options.put("before", lastTime);
			//options.put ("limit", 1);
			if (opts != null)
			{
				options.putAll(opts);
			}
			List<Post> posts;
			if (tag.contains(","))
			{
				posts = tagged(tag.split(",\\s?")[0], options);
			}
			else
			{
				posts = tagged(tag, options);
			}
			if (posts.size() == 0 || posts.isEmpty())
			{
				break;
			}
			loop:
			for (Post post : posts)
			{
				searched++;
				lastTime = post.getTimestamp();
				if (type == null || post.getType().getValue().equals(type))
				{
					if (tag.contains(","))
					{
						for (String s : tag.split(",\\s?"))
						{
							if (!post.getTags().contains(s))
							{
								continue loop;
							}
						}
					}
					else
					{
						for (String t : tagBlacklist)
						{
							if (post.getTags().contains(t)) continue loop;
						}
					}
					if (blogBlacklist.contains(post.getBlogName()) ||
							postBlacklist.contains(post.getId())) { continue; }
					//if (checkBlog) if(!isActiveBlog(post.getBlogName())) {continue;}
					out.put(post, tag);
					postCount++;
				}
			}
		}
		logger.info("Search tag " + tag + " selected " + out.size() + " posts out of " + searched + " searched, took " + (System.currentTimeMillis() - start) + " ms");
		return out;
	}

	private boolean isActiveBlog(String blogName)
	{
		return blogPosts(blogName).size() >= 5;
	}

	public String getBlogName()
	{
		return blogName;
	}

	public Tumblr setBlogName(String blog) throws InvalidBlogNameException
	{
		boolean valid = false;

		for (Blog b : user().getBlogs())
		{
			if (b.getName().equals(blog))
			{
				valid = true;
			}
		}
		if (!valid)
		{
			throw new InvalidBlogNameException(blog);
		}
		this.blogName = blog;
		return this;
	}

	public List<Post> blogDraftPosts()
	{
		return blogDraftPosts(blogName);
	}

	public List<Post> blogDraftPosts(long before)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("before_id", before);
		return blogDraftPosts(blogName, params);
	}

	public List<Post> blogPosts(long before)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("offset", before);
		return blogPosts(blogName, params);
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
				if (post.getType().getValue().equals("answer"))
				{
					asks.add((AnswerPost) post);
				}
			}
		}
		return asks;
	}

	public List<Post> getDrafts()
	{
		long before = 0;
		List<Post> drafts;
		ArrayList<Post> out = new ArrayList<>();
		while ((drafts = blogDraftPosts(before)).size() > 0)
		{
			for (Post post : drafts)
			{
				out.add(post);
				before = post.getId();
			}
		}
		return out;
	}

	public List<Post> getBlogPosts()
	{
		List<Post> posts;
		ArrayList<Post> out = new ArrayList<>();
		while ((posts = blogPosts(out.size())).size() > 0)
		{
			for (Post post : posts)
			{
				out.add(post);
			}
		}
		return out;
	}
}
