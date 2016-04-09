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
import net.walterbarnes.sourcebot.reference.Constants;
import net.walterbarnes.sourcebot.search.SearchExclusion;
import net.walterbarnes.sourcebot.search.SearchInclusion;
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

public class BotThread implements Runnable
{
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

	@Override
	public void run()
	{
		try
		{
			if ((blog.getPostState().equals("draft") && client.getDrafts(url).size() < blog.getPostBuffer()) ||
					(blog.getPostState().equals("queue") && client.getQueuedPosts(url).size() < blog.getPostBuffer()))
			{
				if (blog.getPostState().equals("queue") && !client.blogInfo(url).isAdmin())
				{
					Constants.LOGGER.warning("Bot is not admin on '" + url + "', not running thread");
				}
				Constants.LOGGER.info("Adding posts to queue");

				Map<Post, String> postMap = new HashMap<>();

				List<SearchInclusion> inclusions = blog.getInclusions();
				List<SearchExclusion> exclusions = blog.getExclusions();

				List<String> tagBlacklist = new ArrayList<>();
				List<String> blogBlacklist = new ArrayList<>();

				for (SearchExclusion exclusion : exclusions)
				{
					if (exclusion.isActive())
					{
						switch (exclusion.getType())
						{
							case TAG:
								tagBlacklist.add(exclusion.getTerm());
								break;
							case BLOG:
								blogBlacklist.add(exclusion.getTerm());
								break;
						}
					}
				}

				List<Post> posts = new ArrayList<>();

				for (SearchInclusion inclusion : inclusions)
				{
					SearchTerm t;
					if (inclusion.isActive())
					{
						t = null;
						switch (inclusion.getType())
						{
							case TAG:
								String tag = inclusion.getTerm();
								Constants.LOGGER.info("Getting posts from tag: " + tag);
								if (!terms.containsKey("tag:" + tag))
								{
									terms.put("tag:" + tag, new TagTerm(tag, client, blog));
								}
								t = terms.get("tag:" + tag);
								break;

							case BLOG:
								String b = inclusion.getTerm();
								Constants.LOGGER.info("Getting posts from blog: " + b);
								if (!terms.containsKey("blog:" + b))
								{
									terms.put("blog:" + b, new BlogTerm(b, client, blog));
								}
								t = terms.get("blog:" + b);
								break;
						}
						Map<Post, String> p = t.getPosts(blogBlacklist, tagBlacklist, inclusion);
						postMap.putAll(p);
						posts.addAll(selectPosts(p.keySet(), inclusion.getPostSelect() == null ? blog.getPostSelect() : inclusion.getPostSelect(), 50));
					}
				}

				boolean hasPosted = false;

				if (posts.size() > 0)
				{
					loop:
					while (!hasPosted)
					{
						Constants.LOGGER.info("Selecting post");
						List<Post> p = randomElement(posts, 1, true);

						for (Post post : p)
						{
							if (blog.getPosts().contains(post.getId()))
							{
								Constants.LOGGER.info("Post already used, getting new post");
								hasPosted = false;
								continue;
							}
							if (blog.getCheckBlog() && client.blogPosts(post.getBlogName()).size() < 5)
							{
								Constants.LOGGER.info("Post may be spam, getting new post");
								continue;
							}

							Map<String, Object> params = new HashMap<>();

							params.put("state", blog.getPostState());

							if (!(blog.getPostComment() == null || blog.getPostComment().isEmpty() || blog.getPostComment().equals("null")))
							{ params.put("comment", blog.getPostComment()); }

							List<String> rbTags = new ArrayList<>();

							if (!(blog.getPostTags() == null || blog.getPostTags().length == 0))
							{ Collections.addAll(rbTags, blog.getPostTags()); }

							if (blog.getPreserveTags()) for (String s : post.getTags()) rbTags.add(s);

							params.put("tags", rbTags.size() == 0 ? "" : StringUtils.join(rbTags, ","));
							Constants.LOGGER.info("Attempting to reblog post...");
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
										Constants.LOGGER.warning("Posting failed.");
										failCount++;
									}
								}
								catch (JumblrException e)
								{
									if (failCount > 10) break loop;
									rbd = false;
									hasPosted = false;
									failCount++;
									Constants.LOGGER.log(Level.SEVERE, e.getMessage(), e);
									Thread.sleep(1000);
								}
							}
							String val = postMap.get(post);
							terms.get(val).getCache().remove(post.getId());
							blog.addPost(val.split(":")[0], post.getId(), rb.getId(), val.split(":")[1], post.getBlogName());
						}
					}
				}
			}
		}
		catch (Exception e) { Constants.LOGGER.log(Level.SEVERE, e.getMessage(), e); }
	}

	private List<Post> selectPosts(Collection<Post> posts, String method, int n)
	{
		switch (method)
		{
			case "top":
				return getTopPosts(posts, n);

			case "recent":
				return sortTimestamp(posts, n);

			default:
				return sortTimestamp(posts, n);
		}
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
	private static ArrayList<Post> getTopPosts(Collection<Post> posts, int n)
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
	private static ArrayList<Post> sortTimestamp(Collection<Post> posts, int n)
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
		if (n > p.size())
		{
			return new ArrayList<>(p);
		}
		return new ArrayList<>(p.subList(0, n - 1));
	}

	public class Blog
	{
		private final PreparedStatement getBId;
		private final int id;
		private List<SearchExclusion> exclusions = new ArrayList<>();
		private long exclusionsQTime = 0;
		private List<SearchInclusion> inclusions = new ArrayList<>();
		private long inclusionsQTime = 0;
		private PreparedStatement addPosts;
		private PreparedStatement addStats;

		private PreparedStatement getConfig;
		private ResultSet configRs;
		private long configQTime = 0;

		private PreparedStatement getSI;
		private PreparedStatement getSE;

		private PreparedStatement getPosts;

		Blog(String url) throws SQLException
		{
			getBId = conn.prepareStatement("SELECT id FROM blogs WHERE url = ?");
			getBId.setString(1, url);
			ResultSet rs = getBId.executeQuery();
			rs.next();
			id = rs.getInt("id");

			getConfig = conn.prepareStatement("SELECT * FROM blogs WHERE id = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			getConfig.setInt(1, id);

			getSI = conn.prepareStatement("SELECT DISTINCT * FROM search_inclusions WHERE blog_id = ? ORDER BY id", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			getSI.setInt(1, id);

			getSE = conn.prepareStatement("SELECT DISTINCT * FROM search_exclusions WHERE blog_id = ? ORDER BY id", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			getSE.setInt(1, id);

			getPosts = conn.prepareStatement("SELECT post_id FROM seen_posts WHERE url = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			getPosts.setString(1, url);

			addPosts = conn.prepareStatement("INSERT INTO seen_posts (url, search_type, post_id, rb_id, search_term, blog) VALUES (?, ?, ?, ?, ?, ?)", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			addPosts.setString(1, url);

			addStats = conn.prepareStatement("INSERT INTO search_stats (blog_id, type, term, search_time, searched, selected) VALUES (?, ?, ?, ?, ?, ?)", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			addStats.setInt(1, id);
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

		public String[] getPostType()
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
					return (String[]) configRs.getArray("post_type").getArray();
				}
			}
			catch (SQLException e)
			{
				Constants.LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
				Constants.LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
				Constants.LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
				Constants.LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
				Constants.LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
				Constants.LOGGER.log(Level.SEVERE, e.getMessage(), e);
				return null;
			}
			return null;
		}

		String[] getPostTags()
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
					return (String[]) configRs.getArray("post_tags").getArray();
				}
			}
			catch (SQLException e)
			{
				Constants.LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
				Constants.LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
				Constants.LOGGER.log(Level.SEVERE, e.getMessage(), e);
				return 0;
			}
			return 0;
		}

		public List<SearchExclusion> getExclusions() throws SQLException
		{
			if (System.currentTimeMillis() - exclusionsQTime > 60000)
			{
				ResultSet rs = getSE.executeQuery();
				List<SearchExclusion> out = new ArrayList<>();
				while (rs.next())
				{
					out.add(new SearchExclusion(rs.getInt("id"), rs.getString("type"), rs.getString("term"),
							rs.getBoolean("active")));
				}
				return (exclusions = out);
			}
			return exclusions;
		}

		public List<SearchInclusion> getInclusions() throws SQLException
		{
			if (System.currentTimeMillis() - inclusionsQTime > 60000)
			{
				ResultSet rs = getSI.executeQuery();
				List<SearchInclusion> out = new ArrayList<>();
				while (rs.next())
				{
					int incId = rs.getInt("id");
					String type = rs.getString("type");
					String term = rs.getString("term");
					String[] required_tags = rs.getArray("required_tags") != null ? (String[]) rs.getArray("required_tags").getArray() : null;
					String[] postType = rs.getArray("post_type") != null ? (String[]) rs.getArray("post_type").getArray() : null;
					String postSelect = rs.getString("post_select");
					int sample = rs.getInt("sample_size");
					boolean active = rs.getBoolean("active");
					out.add(new SearchInclusion(incId, type, term, required_tags, postType, postSelect, sample, active));
				}
				return (inclusions = out);
			}
			return inclusions;
		}
	}
}
