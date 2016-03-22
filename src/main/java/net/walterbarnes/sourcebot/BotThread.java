package net.walterbarnes.sourcebot;

import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;
import net.walterbarnes.sourcebot.tumblr.Tumblr;
import org.apache.commons.lang3.StringUtils;

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
	private final Blog blog;
	private final Connection conn;
	private final String url;

	BotThread(Tumblr client, String url, Connection conn) throws InvalidBlogNameException, SQLException
	{
		this.url = url;
		this.client = client;
		this.conn = conn;
		this.blog = new Blog(url);
	}

	/**
	 * Selects pseudo-random elements from a collection
	 *
	 * @param c      Collection to select from
	 * @param n      Number of elements to select
	 * @param unique Whether the selection should be unique
	 * @param <E>    Type of element in ollection
	 * @return Random element(s) from collection
	 */
	private static <E> List<E> randomElement(Collection<E> c, int n, boolean unique)
	{
		List<E> out = new ArrayList<>();
		List<E> p = new ArrayList<>(c);

		while (out.size() < n)
		{
			E post = p.get(ThreadLocalRandom.current().nextInt(0, p.size()));
			out.add(post);
			if (unique) p.remove(post);
		}
		return out;
	}

	/**
	 * Selects the top posts from a set by their note count
	 *
	 * @param posts Set to sort
	 * @param n     Number of top posts to select
	 * @return Top n posts from set
	 */
	private static ArrayList<Post> getTopPosts(Set<Post> posts, int n)
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
		return new ArrayList<>(p.subList(0, n - 1));
	}

	/**
	 * Sorts a set of Tumblr Posts by their timestamp
	 * @param posts Set to sort
	 * @param n Number of posts to return
	 * @return Sorted List of posts
	 */
	private static ArrayList<Post> sortTimestamp(Set<Post> posts, int n)
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

				if (a.getTimestamp() < b.getTimestamp())
				{
					p.set(i - 1, b);
					p.set(i, a);
					moves++;
				}
			}
		}
		return new ArrayList<>(p.subList(0, n - 1));
	}

	@Override
	public void run()
	{
		try
		{
			if ((blog.getPostState().equals("draft") && client.getDrafts(url).size() < blog.getPostBuffer()) ||
					(blog.getPostState().equals("queue") && client.getQueuedPosts(url).size() < blog.getPostBuffer()))
			{
				logger.info("Adding posts to queue");
				Map<Post, String> posts = new HashMap<>();
				List<String> tags = blog.getTagWhitelist();
				for (String tag : tags)
				{
					posts.putAll(client.getPostsFromTag(tag, blog.getPostType(), blog.getSampleSize(), null,
							blog.getBlogBlacklist(), blog.getTagBlacklist(), blog.getPosts(), blog, conn));
				}
				List<Post> p = randomElement(blog.getPostSelect().equals("top") ? getTopPosts(posts.keySet(), 50) :
						sortTimestamp(posts.keySet(), 50), 1, true);
				boolean posted = false;
				for (Post post : p)
				{
					if (blog.getCheckBlog() && client.blogPosts(post.getBlogName()).size() < 5) continue;
					Map<String, Object> params = new HashMap<>();
					params.put("state", blog.getPostState());
					if (!(blog.getPostComment().isEmpty() || blog.getPostComment().equals("null")))
					{
						params.put("comment", blog.getPostComment());
					}
					List<String> rbTags = new ArrayList<>();
					if (!(blog.getPostTags().isEmpty() || blog.getPostTags().equals("null")))
					{
						Collections.addAll(rbTags, blog.getPostTags().split(",\\s?"));
					}
					if (blog.getPreserveTags())
					{
						for (String s : post.getTags())
						{
							rbTags.add(s);
						}
					}
					params.put("tags", rbTags.size() == 0 ? "" : StringUtils.join(rbTags, ","));
					Post rb = null;
					while (!posted)
					{
						try
						{
							rb = post.reblog(url, params);
							if (rb != null) posted = true;
						}
						catch (JumblrException e)
						{
							posted = false;
							logger.log(Level.SEVERE, e.getMessage(), e);
						}
					}
					blog.addPost(post.getId(), rb != null ? rb.getId() : 0, posts.get(post), post.getBlogName());
				}
			}
		}
		catch (Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public class Blog
	{
		private PreparedStatement addPosts;
		private PreparedStatement addStats;

		private PreparedStatement getConfig;
		private ResultSet configRs;
		private long configQTime = 0;

		private PreparedStatement getRules;

		private long tbQTime = 0;
		private List<String> tbList;
		private long twQTime = 0;
		private List<String> twList;
		private long bbQTime = 0;
		private List<String> bbList;
		private long bwQTime = 0;
		private List<String> bwList;

		private PreparedStatement getPosts;

		Blog(String url) throws SQLException
		{
			getConfig = conn.prepareStatement("SELECT * FROM blogs WHERE url = ?;");
			getConfig.setString(1, url);
			getRules = conn.prepareStatement("SELECT DISTINCT term FROM search_rules WHERE url = ? && type = ? && action = ?;");
			getRules.setString(1, url);
			getPosts = conn.prepareStatement("SELECT post_id FROM seen_posts WHERE url = ?;");
			getPosts.setString(1, url);
			addPosts = conn.prepareStatement("INSERT INTO seen_posts (url, post_id, rb_id, tag, blog) VALUES (?, ?, ?, ?, ?)");
			addPosts.setString(1, url);
			addStats = conn.prepareStatement("INSERT INTO tag_stats (url, tag, search_time, search, selected) VALUES (?, ?, ?, ?, ?);");
			addStats.setString(1, url);
		}

		public boolean addStat(String tag, int time, int searched, int selected) throws SQLException
		{
			addStats.setString(2, tag);
			addStats.setInt(3, time);
			addStats.setInt(4, searched);
			addStats.setInt(5, selected);
			return addStats.execute();
		}

		boolean addPost(long id, long rbId, String tag, String blogName) throws SQLException
		{
			addPosts.setLong(2, id);
			addPosts.setLong(3, rbId);
			addPosts.setString(4, tag);
			addPosts.setString(5, blogName);
			return addPosts.execute();
		}

		List<Long> getPosts() throws SQLException
		{
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
				if (System.currentTimeMillis() - configQTime > 60000)
				{
					configRs = getConfig.executeQuery();
					configQTime = System.currentTimeMillis();
				}
				configRs.beforeFirst();
				if (configRs.next())
				{
					if (configRs.getString("post_type").isEmpty() || configRs.getString("post_type").equals("null"))
					{
						return null;
					}
					else
					{
						return configRs.getString("post_type");
					}
				}
			}
			catch (SQLException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				return null;
			}
			return null;
		}

		boolean getCheckBlog()
		{
			try
			{
				if (System.currentTimeMillis() - configQTime > 60000)
				{
					configRs = getConfig.executeQuery();
					configQTime = System.currentTimeMillis();
				}
				configRs.beforeFirst();
				if (configRs.next())
				{
					return configRs.getBoolean("blog_check_active");
				}
			}
			catch (SQLException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				return false;
			}
			return false;
		}

		boolean getPreserveTags()
		{
			try
			{
				if (System.currentTimeMillis() - configQTime > 60000)
				{
					configRs = getConfig.executeQuery();
					configQTime = System.currentTimeMillis();
				}
				configRs.beforeFirst();
				if (configRs.next())
				{
					return configRs.getBoolean("preserve_tags");
				}
			}
			catch (SQLException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				return false;
			}
			return false;
		}

		String getPostSelect()
		{
			try
			{
				if (System.currentTimeMillis() - configQTime > 60000)
				{
					configRs = getConfig.executeQuery();
					configQTime = System.currentTimeMillis();
				}
				configRs.beforeFirst();
				if (configRs.next())
				{
					return configRs.getString("post_select");
				}
			}
			catch (SQLException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				return null;
			}
			return null;
		}

		String getPostState()
		{
			try
			{
				if (System.currentTimeMillis() - configQTime > 60000)
				{
					configRs = getConfig.executeQuery();
					configQTime = System.currentTimeMillis();
				}
				configRs.beforeFirst();
				if (configRs.next())
				{
					return configRs.getString("post_state");
				}
			}
			catch (SQLException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				return null;
			}
			return null;
		}

		String getPostComment()
		{
			try
			{
				if (System.currentTimeMillis() - configQTime > 60000)
				{
					configRs = getConfig.executeQuery();
					configQTime = System.currentTimeMillis();
				}
				configRs.beforeFirst();
				if (configRs.next())
				{
					return configRs.getString("post_comment");
				}
			}
			catch (SQLException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				return null;
			}
			return null;
		}

		String getPostTags()
		{
			try
			{
				if (System.currentTimeMillis() - configQTime > 60000)
				{
					configRs = getConfig.executeQuery();
					configQTime = System.currentTimeMillis();
				}
				configRs.beforeFirst();
				if (configRs.next())
				{
					return configRs.getString("post_tags");
				}
			}
			catch (SQLException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				return null;
			}
			return null;
		}

		int getSampleSize()
		{
			try
			{
				if (System.currentTimeMillis() - configQTime > 60000)
				{
					configRs = getConfig.executeQuery();
					configQTime = System.currentTimeMillis();
				}
				configRs.beforeFirst();
				if (configRs.next())
				{
					return configRs.getInt("sample_size");
				}
			}
			catch (SQLException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				return 0;
			}
			return 0;
		}

		int getPostBuffer()
		{
			try
			{
				if (System.currentTimeMillis() - configQTime > 60000)
				{
					configRs = getConfig.executeQuery();
					configQTime = System.currentTimeMillis();
				}
				configRs.beforeFirst();
				if (configRs.next())
				{
					return configRs.getInt("post_buffer");
				}
			}
			catch (SQLException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				return 0;
			}
			return 0;
		}

		List<String> getBlogBlacklist() throws SQLException
		{
			if (System.currentTimeMillis() - bbQTime > 60000)
			{
				getRules.setString(2, "blog");
				getRules.setString(3, "block");
				ResultSet rs = getRules.executeQuery();
				List<String> out = new ArrayList<>();
				while (rs.next())
				{
					out.add(rs.getString("term"));
				}
				return (bbList = out);
			}
			return bbList;
		}

		public List<String> getBlogWhitelist() throws SQLException
		{
			if (System.currentTimeMillis() - bwQTime > 60000)
			{
				getRules.setString(2, "blog");
				getRules.setString(3, "allow");
				ResultSet rs = getRules.executeQuery();
				List<String> out = new ArrayList<>();
				while (rs.next())
				{
					out.add(rs.getString("term"));
				}
				return (bwList = out);
			}
			return bwList;
		}

		List<String> getTagBlacklist() throws SQLException
		{
			if (System.currentTimeMillis() - tbQTime > 60000)
			{
				getRules.setString(2, "tag");
				getRules.setString(3, "block");
				ResultSet rs = getRules.executeQuery();
				List<String> out = new ArrayList<>();
				while (rs.next())
				{
					out.add(rs.getString("term"));
				}
				return (tbList = out);
			}
			return tbList;
		}

		List<String> getTagWhitelist() throws SQLException
		{
			if (System.currentTimeMillis() - twQTime > 60000)
			{
				getRules.setString(2, "tag");
				getRules.setString(3, "allow");
				ResultSet rs = getRules.executeQuery();
				List<String> out = new ArrayList<>();
				while (rs.next())
				{
					out.add(rs.getString("term"));
				}
				return (twList = out);
			}
			return twList;
		}
	}
}
