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

package net.walterbarnes.sourcebot;

import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;
import net.walterbarnes.sourcebot.tumblr.BlogTerm;
import net.walterbarnes.sourcebot.tumblr.SearchTerm;
import net.walterbarnes.sourcebot.tumblr.TagTerm;
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
	private Map<String, SearchTerm> terms = new HashMap<>();

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
	 * @param <E>    Type of element in collection
	 * @return Random element(s) from collection
	 */
	private static <E> List<E> randomElement(Collection<E> c, int n, boolean unique)
	{
		List<E> out = new ArrayList<>();
		List<E> l = new ArrayList<>(c);

		while (out.size() < n)
		{
			E e = l.get(ThreadLocalRandom.current().nextInt(0, l.size()));
			out.add(e);
			if (unique) l.remove(e);
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
	 *
	 * @param posts Set to sort
	 * @param n     Number of posts to return
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
				List<String> blogs = blog.getBlogWhitelist();

//				for (String tag : tags)
//				{
//					logger.info("Getting posts for tag: " + tag);
//					int startSize = posts.size();
//					if (System.currentTimeMillis() - cacheTime > (30 * 60 * 1000) || !cache.containsKey("tag:" + tag))
//					{
//						logger.info("Invalidating expired cache");
//						Map<Post, String> p = client.getPostsFromTag(tag, null, blog);
//						posts.putAll(p);
//						if (!cache.containsKey("tag:" + tag))
//						{
//							List<Post> list = new ArrayList<>();
//							cache.put("tag:" + tag, list);
//						}
//						cache.get("tag:" + tag).clear();
//						for (Post pst : p.keySet())
//						{
//							cache.get(p.get(pst)).add(pst);
//						}
//					}
//					else
//					{
//						logger.info("Using cached posts");
//						for (Post p : cache.get("tag:" + tag))
//						{
//							posts.put(p, "tag:" + tag);
//						}
//					}
//					System.out.println(posts.size() - startSize);
//				}

				for (String tag : tags)
				{
					logger.info("Getting posts from tag: " + tag);
					if (!terms.containsKey("tag:" + tag))
					{
						terms.put("tag:" + tag, new TagTerm(tag, client, logger));
					}
					posts.putAll(terms.get("tag:" + tag).getPosts(null, blog));
				}

				for (String b : blogs)
				{
					logger.info("Getting posts from tag: " + b);
					if (!terms.containsKey("blog:" + b))
					{
						terms.put("blog:" + b, new BlogTerm(b, client, logger));
					}
					posts.putAll(terms.get("blog:" + b).getPosts(null, blog));
				}

//				for (String b : blogs)
//				{
//					logger.info("Getting posts from blog: " + b);
//					int startSize = posts.size();
//					if (System.currentTimeMillis() - cacheTime > (30 * 60 * 1000))
//					{
//						logger.info("Invalidating expired cache");
//						Map<Post, String> p = client.getPostsFromBlog(b, null, blog);
//						posts.putAll(p);
//						if (!cache.containsKey("blog:" + b))
//						{
//							List<Post> list = new ArrayList<>();
//							cache.put("blog:" + b, list);
//						}
//						for (Post pst : p.keySet())
//						{
//							cache.get(p.get(pst)).add(pst);
//						}
//					}
//					else
//					{
//						logger.info("Using cached posts");
//						for (Post p : cache.get("blog:" + b))
//						{
//							posts.put(p, "blog:" + b);
//						}
//					}
//					System.out.println(posts.size() - startSize);
//				}

				//for (String b : blogs) posts.putAll(client.getPostsFromBlog(b, null, blog));
				boolean hasPosted = false;

				loop:
				while (!hasPosted)
				{
					logger.info("Selecting post");
					List<Post> p = randomElement(blog.getPostSelect().equals("top") ? getTopPosts(posts.keySet(), 50) :
							sortTimestamp(posts.keySet(), 50), 1, true);

					for (Post post : p)
					{
						if (blog.getPosts().contains(post.getId()))
						{
							logger.info("Post already used, getting new post");
							hasPosted = false;
							continue;
						}
						if (blog.getCheckBlog() && client.blogPosts(post.getBlogName()).size() < 5)
						{
							logger.info("Post may be spam, getting new post");
							continue;
						}

						Map<String, Object> params = new HashMap<>();

						params.put("state", blog.getPostState());

						if (!(blog.getPostComment().isEmpty() || blog.getPostComment().equals("null")))
						{ params.put("comment", blog.getPostComment()); }

						List<String> rbTags = new ArrayList<>();

						if (!(blog.getPostTags().isEmpty() || blog.getPostTags().equals("null")))
						{ Collections.addAll(rbTags, blog.getPostTags().split(",\\s?")); }

						if (blog.getPreserveTags()) for (String s : post.getTags()) rbTags.add(s);

						params.put("tags", rbTags.size() == 0 ? "" : StringUtils.join(rbTags, ","));
						logger.info("Attempting to reblog post...");
						Post rb = null;
						boolean rbd = false;
						int failCount = 0;
						while (!rbd)
						{
							try
							{
								rb = post.reblog(url, params);
								if (rb != null)
								{
									rbd = true;
									hasPosted = true;
								}
								else
								{
									logger.warning("Posting failed.");
									failCount++;
								}
							}
							catch (JumblrException e)
							{
								if (failCount > 10) break loop;
								rbd = hasPosted = false;
								failCount++;
								logger.log(Level.SEVERE, e.getMessage(), e);
								Thread.sleep(1000);
							}
						}
						String val = posts.get(post);
						blog.addPost(val.split(":")[0], post.getId(), rb.getId(), val.split(":")[1], post.getBlogName());
					}
				}
			}
		}
		catch (Exception e) { logger.log(Level.SEVERE, e.getMessage(), e); }
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

			addPosts = conn.prepareStatement("INSERT INTO seen_posts (url, search_type, post_id, rb_id, tag, blog) VALUES (?, ?, ?, ?, ?, ?)");
			addPosts.setString(1, url);

			addStats = conn.prepareStatement("INSERT INTO tag_stats (url, search_type, tag, search_time, search, selected) VALUES (?, ?, ?, ?, ?, ?);");
			addStats.setString(1, url);
		}

		public boolean addStat(String type, String tag, int time, int searched, int selected) throws SQLException
		{
			addStats.setString(2, type);
			addStats.setString(3, tag);
			addStats.setInt(4, time);
			addStats.setInt(5, searched);
			addStats.setInt(6, selected);
			return addStats.execute();
		}

		boolean addPost(String type, long id, long rbId, String tag, String blogName) throws SQLException
		{
			addPosts.setString(2, type);
			addPosts.setLong(3, id);
			addPosts.setLong(4, rbId);
			addPosts.setString(5, tag);
			addPosts.setString(6, blogName);
			return addPosts.execute();
		}

		public List<Long> getPosts() throws SQLException
		{
			List<Long> out = new ArrayList<>();
			ResultSet rs = getPosts.executeQuery();
			while (rs.next()) out.add(rs.getLong("post_id"));
			return out;
		}

		public String getPostType()
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
					return configRs.getString("post_type").isEmpty() || configRs.getString("post_type").equals("null") ?
							null : configRs.getString("post_type");
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
				{ return configRs.getBoolean("blog_check_active"); }
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
				{ return configRs.getBoolean("preserve_tags"); }
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
				{ return configRs.getString("post_select"); }
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
				{ return configRs.getString("post_state"); }
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
				{ return configRs.getString("post_comment"); }
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
				{ return configRs.getString("post_tags"); }
			}
			catch (SQLException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				return null;
			}
			return null;
		}

		public int getSampleSize()
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
				{ return configRs.getInt("sample_size"); }
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
				{ return configRs.getInt("post_buffer"); }
			}
			catch (SQLException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				return 0;
			}
			return 0;
		}

		public List<String> getBlogBlacklist() throws SQLException
		{
			if (System.currentTimeMillis() - bbQTime > 60000)
			{
				getRules.setString(2, "blog");
				getRules.setString(3, "block");
				ResultSet rs = getRules.executeQuery();
				List<String> out = new ArrayList<>();
				while (rs.next())
					out.add(rs.getString("term"));
				return (bbList = out);
			}
			return bbList;
		}

		@SuppressWarnings ("unused")
		public List<String> getBlogWhitelist() throws SQLException
		{
			if (System.currentTimeMillis() - bwQTime > 60000)
			{
				getRules.setString(2, "blog");
				getRules.setString(3, "allow");
				ResultSet rs = getRules.executeQuery();
				List<String> out = new ArrayList<>();
				while (rs.next())
					out.add(rs.getString("term"));
				return (bwList = out);
			}
			return bwList;
		}

		public List<String> getTagBlacklist() throws SQLException
		{
			if (System.currentTimeMillis() - tbQTime > 60000)
			{
				getRules.setString(2, "tag");
				getRules.setString(3, "block");
				ResultSet rs = getRules.executeQuery();
				List<String> out = new ArrayList<>();
				while (rs.next())
					out.add(rs.getString("term"));
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
					out.add(rs.getString("term"));
				return (twList = out);
			}
			return twList;
		}
	}
}
