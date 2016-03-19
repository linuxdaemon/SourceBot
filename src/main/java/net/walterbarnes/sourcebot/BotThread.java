package net.walterbarnes.sourcebot;

import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;
import net.walterbarnes.sourcebot.tumblr.Tumblr;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BotThread implements Runnable
{
	private static final Logger logger = Logger.getLogger(SourceBot.class.getName());
	private final Tumblr client;
	private final String url;
	private final Blog blog;
	private final Connection conn;

	public BotThread(Tumblr client, String url, Connection conn) throws InvalidBlogNameException, SQLException
	{
		this.client = client;
		this.url = url;
		this.conn = conn;
		this.blog = new Blog(this.url);
		client.setBlogName(url);
	}

	private static ArrayList<Post> selectPosts(Collection<Post> posts, int n, boolean unique)
	{
		ArrayList<Post> out = new ArrayList<>();
		List<Post> p = new ArrayList<>(posts);

		while (out.size() < n)
		{
			Post post = p.get(ThreadLocalRandom.current().nextInt(0, p.size() + 1));
			out.add(post);
			if (unique) p.remove(post);
		}
		return out;
	}

	private static ArrayList<Post> getTopPosts(Set<Post> posts)
	{
		int moves = 0;
		boolean firstRun = true;
		List<Post> p = new ArrayList<>(posts);
		while (firstRun || moves > 0)
		{
			moves = 0;
			firstRun = false;
			for (int i = 1; i < p.size(); i++)
			{
				Post a = p.get(i - 1);
				Post b = p.get(i);

				if (a.getNoteCount() < b.getNoteCount())
				{
					p.set(i - 1, b);
					p.set(i, a);
					moves++;
				}
			}
		}
		return new ArrayList<>(p.subList(0, 49));
	}

	@Override
	public void run()
	{
		try
		{
			if (client.blogDraftPosts().size() < 20)
			{
				logger.info("Adding posts to queue");
				Map<Post, String> posts = new HashMap<>();
				for (String tag : blog.getTagWhitelist())
				{
					posts.putAll(client.getPostsFromTag(tag, blog.getPostType(), blog.getSampleSize(), null,
							blog.getBlogBlacklist(), blog.getTagBlacklist(), blog.getPosts()));
				}
				for (Post post : selectPosts(blog.getPostSelect().equals("top") ?
						getTopPosts(posts.keySet()) : posts.keySet(), 1, true))
				{
					Map<String, Object> params = new HashMap<>();
					params.put("state", blog.getPostState());
					params.put("comment", blog.getPostComment());
					params.put("tags", blog.getPostTags());
					try
					{
						Post rb = post.reblog(client.getBlogName(), params);
					}
					catch (NullPointerException e)
					{
						logger.log(Level.SEVERE, e.getMessage(), e);
					}
					blog.addPost(post.getId(), posts.get(post), post.getBlogName());
				}
			}
		}
		catch (JumblrException ignored) {}
		catch (Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	private class Blog
	{
		private final String url;
		private PreparedStatement addPosts;
		private PreparedStatement getConfig;
		private PreparedStatement getRules;
		private PreparedStatement getPosts;

		Blog(String url) throws SQLException
		{
			this.url = url;
			getConfig = conn.prepareStatement("SELECT * FROM blogs WHERE url = ?;");
			getRules = conn.prepareStatement("SELECT DISTINCT term FROM search_rules WHERE url = ? && type = ? && action = ?;");
			getPosts = conn.prepareStatement("SELECT post_id FROM seen_posts WHERE url = ?;");
			addPosts = conn.prepareStatement("INSERT INTO seen_posts (url, post_id, tag, blog) VALUES (?, ?, ?, ?)");
		}

		boolean addPost(long id, String tag, String blogName) throws SQLException
		{
			addPosts.setString(1, url);
			addPosts.setLong(2, id);
			addPosts.setString(3, tag);
			addPosts.setString(4, blogName);
			return addPosts.execute();
		}

		List<Long> getPosts() throws SQLException
		{
			getPosts.setString(1, url);
			List<Long> out = new ArrayList<>();
			ResultSet rs = getPosts.executeQuery();
			while (rs.next())
			{
				out.add(rs.getLong("post_id"));
			}
			return out;
		}

		String getPostType()
		{
			try
			{
				getConfig.setString(1, url);
				ResultSet rs = getConfig.executeQuery();
				while (rs.next())
				{
					return rs.getString("post_type");
				}
			}
			catch (SQLException e)
			{
				return null;
			}
			return null;
		}

		String getPostSelect()
		{
			try
			{
				getConfig.setString(1, url);
				ResultSet rs = getConfig.executeQuery();
				while (rs.next())
				{
					return rs.getString("post_select");
				}
			}
			catch (SQLException e)
			{
				return null;
			}
			return null;
		}

		String getPostState()
		{
			try
			{
				getConfig.setString(1, url);
				ResultSet rs = getConfig.executeQuery();
				while (rs.next())
				{
					return rs.getString("post_state");
				}
			}
			catch (SQLException e)
			{
				return null;
			}
			return null;
		}

		String getPostComment()
		{
			try
			{
				getConfig.setString(1, url);
				ResultSet rs = getConfig.executeQuery();
				while (rs.next())
				{
					return rs.getString("post_comment");
				}
			}
			catch (SQLException e)
			{
				return null;
			}
			return null;
		}

		String getPostTags()
		{
			try
			{
				getConfig.setString(1, url);
				ResultSet rs = getConfig.executeQuery();
				while (rs.next())
				{
					return rs.getString("post_tags");
				}
			}
			catch (SQLException e)
			{
				return null;
			}
			return null;
		}

		int getSampleSize()
		{
			try
			{
				getConfig.setString(1, url);
				ResultSet rs = getConfig.executeQuery();
				if (rs.next())
				{
					return rs.getInt("sample_size");
				}
			}
			catch (SQLException e)
			{
				return 0;
			}
			return 0;
		}

		List<String> getBlogBlacklist() throws SQLException
		{
			getRules.setString(1, url);
			getRules.setString(2, "blog");
			getRules.setString(3, "block");
			ResultSet rs = getRules.executeQuery();
			List<String> out = new ArrayList<>();
			while (rs.next())
			{
				out.add(rs.getString("term"));
			}
			return out;
		}

		public List<String> getBlogWhitelist() throws SQLException
		{
			getRules.setString(1, url);
			getRules.setString(2, "blog");
			getRules.setString(3, "allow");
			ResultSet rs = getRules.executeQuery();
			List<String> out = new ArrayList<>();
			while (rs.next())
			{
				out.add(rs.getString("term"));
			}
			return out;
		}

		List<String> getTagBlacklist() throws SQLException
		{
			getRules.setString(1, url);
			getRules.setString(2, "tag");
			getRules.setString(3, "block");
			ResultSet rs = getRules.executeQuery();
			List<String> out = new ArrayList<>();
			while (rs.next())
			{
				out.add(rs.getString("term"));
			}
			return out;
		}

		List<String> getTagWhitelist() throws SQLException
		{
			getRules.setString(1, url);
			getRules.setString(2, "tag");
			getRules.setString(3, "allow");
			ResultSet rs = getRules.executeQuery();
			List<String> out = new ArrayList<>();
			while (rs.next())
			{
				out.add(rs.getString("term"));
			}
			return out;
		}
	}
}
