package net.walterbarnes.sourcebot.tumblr;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.AnswerPost;
import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.BotThread;

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

	public Tumblr(String consumerKey, String consumerSecret, String token, String tokenSecret, Logger logger)
	{
		super(consumerKey, consumerSecret);
		this.logger = logger;
		setToken(token, tokenSecret);
	}

	public Map<Post, String> getPostsFromTag(String tag, String type, int postNum, HashMap<String, Object> opts,
											 List<String> blogBlacklist, List<String> tagBlacklist,
											 List<Long> postBlacklist, BotThread.Blog blog, Connection conn) throws SQLException
	{
		int postCount = 0;
		int searched = 0;
		long lastTime = System.currentTimeMillis() / 1000;
		long start = System.currentTimeMillis();
		Map<Post, String> out = new HashMap<>();
		logger.info("Searching tag " + tag);
		while (postCount < postNum)
		{
			HashMap<String, Object> options = new HashMap<>();
			options.put("before", lastTime);
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
					out.put(post, tag);
					postCount++;
				}
			}
		}
		logger.info(String.format("Searched tag %s, selected %d posts out of %d searched (%f%%), took %d ms", tag, out.size(), searched, ((double) (((float) out.size()) / ((float) searched)) * 100), System.currentTimeMillis() - start));
		blog.addStat(tag, (int) (System.currentTimeMillis() - start), searched, out.size());
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

	public List<Post> blogQueuedPosts(String blogName, long offset)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("offset", offset);
		return blogDraftPosts(blogName, params);
	}

	public List<Post> blogPosts(String blogName, long before)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("offset", before);
		return blogPosts(blogName, params);
	}

	public List<Post> blogSubmissions(String blogName, int offset)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("offset", offset);
		return blogSubmissions(blogName, params);
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
				if (post.getType().getValue().equals("answer"))
				{
					asks.add((AnswerPost) post);
				}
			}
		}
		return asks;
	}

	public List<Post> getQueuedPosts(String blogName)
	{
		long offset = 0;
		List<Post> queue;
		ArrayList<Post> out = new ArrayList<>();
		while ((queue = blogQueuedPosts(blogName, offset)).size() > 0)
		{
			for (Post post : queue)
			{
				out.add(post);
				offset++;
			}
		}
		return out;
	}

	public List<Post> getDrafts(String blogName)
	{
		long before = 0;
		List<Post> drafts;
		ArrayList<Post> out = new ArrayList<>();
		while ((drafts = blogDraftPosts(blogName, before)).size() > 0)
		{
			for (Post post : drafts)
			{
				out.add(post);
				before = post.getId();
			}
		}
		return out;
	}

	public List<Post> getBlogPosts(String blogName)
	{
		List<Post> posts;
		ArrayList<Post> out = new ArrayList<>();
		while ((posts = blogPosts(blogName, out.size())).size() > 0)
		{
			for (Post post : posts)
			{
				out.add(post);
			}
		}
		return out;
	}
}
