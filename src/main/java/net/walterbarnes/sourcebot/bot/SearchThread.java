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

package net.walterbarnes.sourcebot.bot;

import com.github.onyxfoxdevelopment.collections.CollectionHelper;
import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.bot.search.PostComparator;
import net.walterbarnes.sourcebot.bot.search.SearchExclusion;
import net.walterbarnes.sourcebot.bot.search.SearchInclusion;
import net.walterbarnes.sourcebot.bot.search.SearchRule;
import net.walterbarnes.sourcebot.common.config.types.BlogConfig;
import net.walterbarnes.sourcebot.common.tumblr.*;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings ("SameParameterValue")
public class SearchThread implements Runnable
{
	private static final Logger logger = Logger.getLogger(SearchThread.class.getName());
	public final BlogConfig blog;
	private final Tumblr client;
	private final String url;
	private final Map<String, SearchTerm> terms = new HashMap<>();

	SearchThread(Tumblr client, String url, Connection conn) throws SQLException
	{
		this.url = url;
		this.client = client;
		this.blog = new BlogConfig(client, conn, url);
	}

	@Override
	public void run()
	{
		try
		{
			// Check the blog's configured posting state and check if we need to post some more posts
			if (!blog.isPostBufFull())
			{
				logger.info(String.format("[%s] %d posts in queue", url, client.getQueuedPosts(url).size()));
				logger.info("Adding posts to queue");

				// A map of all the posts we pull from the tags/blogs, linked with their search terms
				Map<Post, String> postMap = new HashMap<>();

				// Load all search rules from the database
				List<SearchRule> rules = blog.getSearchRules().stream().filter(SearchRule::isActive)
						.collect(Collectors.toList());
				logger.info(String.format("%d search rules loaded.", rules.size()));

				List<SearchExclusion> exclusions = rules.stream()
						.filter(rule -> rule.getAction() == SearchRule.RuleAction.EXCLUDE)
						.map(rule -> (SearchExclusion) rule).collect(Collectors.toList());

				List<SearchInclusion> inclusions = rules.stream()
						.filter(rule -> rule.getAction() == SearchRule.RuleAction.INCLUDE)
						.map(rule -> (SearchInclusion) rule).collect(Collectors.toList());

				List<String> tagBlacklist = exclusions.stream()
						.filter(rule -> rule.getType() == SearchRule.SearchType.TAG)
						.map(SearchExclusion::getTerm).collect(Collectors.toList());

				List<String> blogBlacklist = exclusions.stream()
						.filter(rule -> rule.getType() == SearchRule.SearchType.BLOG)
						.map(SearchExclusion::getTerm).collect(Collectors.toList());

				List<Post> posts = new ArrayList<>();

				// Load all inclusions in to the terms map

				inclusions.stream()
						.filter(rule -> !terms.containsKey(rule.getFullTerm()) && rule.getType() == SearchRule.SearchType.TAG)
						.forEach(rule -> terms.put(rule.getFullTerm(), new TagTerm(rule.getTerm())));

				inclusions.stream()
						.filter(rule -> !terms.containsKey(rule.getFullTerm()) && rule.getType() == SearchRule.SearchType.BLOG)
						.forEach(rule -> terms.put(rule.getFullTerm(), new BlogTerm(rule.getTerm())));

				for (SearchInclusion inclusion : inclusions)
				{
					logger.info(String.format("Getting posts from %s: %s", inclusion.getType().getName(), inclusion.getTerm()));

					SearchTerm t = terms.get(inclusion.getFullTerm());
					Map<Post, String> p = t.getPosts(blogBlacklist, tagBlacklist, inclusion);
					postMap.putAll(p);
					posts.addAll(selectPosts(p.keySet(), inclusion.getPostSelect() != null ? inclusion.getPostSelect() : blog.getPostSelect(), 50));
				}

				boolean hasPosted = false;

				// Sometimes we won't get any posts from a search, and that's probably ok
				if (posts.size() > 0)
				{
					loop:
					while (!hasPosted)
					{
						logger.info("Selecting post");
						Collection<Post> p = CollectionHelper.randomElement(posts, 1, true);

						for (Post post : p)
						{
							if (blog.getPosts().contains(post.getId()))
							{
								logger.info("Post already used, getting new post");
								hasPosted = false;
								continue;
							}

							// If the user has it enabled for this blog, check if the post might be a spam posts
							// TODO implement spam match rules
							if (blog.getCheckBlog() && (client.blogPosts(post.getBlogName()).size() < 5 ||
									client.blogInfo(post.getBlogName()).getTitle().equals("Без названия")))
							{
								logger.info("Post may be spam, getting new post");
								continue;
							}

							Map<String, Object> params = new HashMap<>();

							params.put("state", blog.getPostState());

							if (!(blog.getPostComment() == null || blog.getPostComment().isEmpty() || blog.getPostComment().equals("null")))
							{
								params.put("comment", blog.getPostComment());
							}

							List<String> rbTags = new ArrayList<>();

							if (!(blog.getPostTags() == null || blog.getPostTags().length == 0))
							{
								Collections.addAll(rbTags, blog.getPostTags());
							}

							if (blog.getPreserveTags()) rbTags.addAll(post.getTags());

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
									rbd = false;
									hasPosted = false;
									failCount++;
									logger.log(Level.SEVERE, e.getMessage(), e);
									if (failCount > 10) break loop;
									Thread.sleep(1000);
								}
							}
							String val = postMap.get(post);
							logger.info(val);
							logger.info(terms.toString());
							ISearchTerm t = terms.get(val);
							PostCache c = t.getCache();
							c.remove(post.getId());
							blog.addPost(val.split(":")[0], post.getId(), rb.getId(), val.split(":")[1], post.getBlogName());
						}
					}
				}
			}
		}
		catch (InterruptedException ignored) {}
		catch (RuntimeException e)
		{
			throw new RuntimeException(e);
		}
		catch (Exception e) { logger.log(Level.SEVERE, e.getMessage(), e); }
	}

	private List<Post> selectPosts(Collection<Post> posts, String method, int n) throws InstantiationException, IllegalAccessException
	{
		switch (method)
		{
			case "top":
				return new ArrayList<>(CollectionHelper.cutMaxLen(CollectionHelper.sortByCompare(posts, PostComparator.NoteCount.class, true), n));

			default:
				return new ArrayList<>(CollectionHelper.cutMaxLen(CollectionHelper.sortByCompare(posts, PostComparator.Timestamp.class, true), n));
		}
	}
}
