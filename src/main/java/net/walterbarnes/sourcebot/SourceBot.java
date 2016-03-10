package net.walterbarnes.sourcebot;

import com.tumblr.jumblr.types.AnswerPost;
import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.config.Config;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;
import net.walterbarnes.sourcebot.tumblr.Tumblr;
import net.walterbarnes.sourcebot.util.LogHelper;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SourceBot
{
	public static void main (String[] args)
	{
		LogHelper.init();
		Logger logger = LogHelper.getLogger();
		Config.load();
		Tumblr client = new Tumblr (Config.getConsumerKey (), Config.getConsumerSecret (),
				Config.getToken(), Config.getTokenSecret(), logger);

		try
		{
			client.setBlogName (Config.getBlogUrl ());
		}
		catch (InvalidBlogNameException e)
		{
			e.printStackTrace ();
		}

		if (args.length > 0 && Arrays.asList(args).contains("purgeDrafts"))
		{
			List<Post> drafts;
			while ((drafts = client.blogDraftPosts()).size() > 0)
			{
				logger.info(String.format("Purging %d Posts from Drafts", drafts.size()));
				for (Post post : drafts)
				{
					logger.fine(String.format("Deleting post with id '%d' from drafts", post.getId()));
					post.delete();
				}
			}
			System.exit(0);
		}

		while (true)
		{
			if (client.blogSubmissions().size() > 0)
			{
				logger.info("Parsing Submissions");
				List<AnswerPost> asks = client.getAsks();
				for (AnswerPost ask : asks)
				{
					logger.info("Processing ask");
					if (Arrays.asList(Config.getBlogAdmins()).contains(ask.getAskingName()))
					{
						String[] words;
						if ((words = ask.getQuestion().split(" ")).length > 1 && words[0].matches("[Cc]onfig"))
						{
							switch (words[1])
							{
								case "blogblacklist":
									if (words.length > 2)
									{
										switch (words[2])
										{
											case "add":
												if (words.length > 3)
												{
													logger.info("Adding blogs to blacklist");
													for (int i = 3; i < words.length; i++)
													{
														List<String> bbl = Config.getBlogBlacklist();
														logger.fine("Adding " + words[i] + " to blacklist");
														bbl.add(words[i]);
														Config.load();
														Config.setBlogBlacklist(bbl);
													}
												}
												break;
										}
									}
									break;

								case "tagsearch":
									if (words.length > 2)
									{
										switch (words[2])
										{
											case "add":
												if (words.length > 3)
												{
													logger.info("Adding tags to search");
													for (String tag : ask.getQuestion()
															.replace("config tagsearch add ", "").split(","))
													{
														List<String> tags = Config.getTags();
														logger.fine("Adding " + tag + " to search list");
														tags.add(tag.trim());
														Config.load();
														Config.setTags(tags);
													}
												}
												break;
										}
									}
									break;

								case "tagblacklist":
									if (words.length > 2)
									{
										switch (words[2])
										{
											case "add":
												if (words.length > 3)
												{
													logger.info("Adding tags to blacklist");
													for (String tag : ask.getQuestion()
															.replace("config tagblacklist add ", "").split(","))
													{
														List<String> bbl = Config.getTagBlacklist();
														logger.fine("Adding tag '" + tag + "' to blacklist");
														bbl.add(tag.trim());
														Config.load();
														Config.setTagBlacklist(bbl);
													}
												}
												break;
										}
									}
									break;
							}
						}
					}
					logger.fine("Deleting ask with id " + ask.getId());
					ask.delete();
				}
			}
			if (!(args.length > 0 && Arrays.asList(args).contains("noPost")) )
			{
				try
				{
					while (client.blogDraftPosts().size() < 20)
					{
						Config.load();
						logger.info("Adding posts to queue");
						ArrayList<Post> posts = new ArrayList<> ();
						for (String tag : Config.getTags ())
						{
							//System.out.println("Searching tag " + tag);
							posts.addAll (client.getPostsFromTag (tag, "text",
									args.length > 0 && args[0].equals("debug") ? 10 : 1000, null,
									Config.getBlogBlacklist(), Config.getTagBlacklist(),
									Config.getPostBlacklist()));
						}
						for (Post post : selectPosts (getTopPosts (posts), 1, true))
						{
							Map<String, Object> params = new HashMap<> ();
							params.put("state", "draft");
							params.put ("comment", "Source?");
							try
							{
								if (!(args.length > 0) || !args[0].equals("debug"))
									post.reblog(client.getBlogName(), params);
							}
							catch (NullPointerException e)
							{
								logger.log(Level.SEVERE, e.getMessage(), e);
							}
							List<Long> pbl = Config.getPostBlacklist();
							pbl.add(post.getId());
							Config.load();
							Config.setPostBlacklist(pbl);
						}
					}
					Thread.sleep(10000);
				}
				catch (Exception e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}

	private static ArrayList<Post> selectPosts (ArrayList<Post> posts, int n, boolean unique)
	{
		ArrayList<Post> out = new ArrayList<> ();

		while (out.size () < n)
		{
			Post post = posts.get (ThreadLocalRandom.current ().nextInt (0, posts.size () + 1));
			out.add (post);
			if (unique) posts.remove (post);
		}
		return out;
	}

	private static ArrayList<Post> getTopPosts (ArrayList<Post> posts)
	{
		int moves = 0;
		boolean firstRun = true;
		while (firstRun || moves > 0)
		{
			moves = 0;
			firstRun = false;
			for (int i = 1; i < posts.size (); i++)
			{
				Post a = posts.get (i - 1);
				Post b = posts.get (i);

				if (a.getNoteCount () < b.getNoteCount ())
				{
					posts.set (i - 1, b);
					posts.set (i, a);
					moves++;
				}
			}
		}

		return new ArrayList<> (posts.subList (0, 49));
	}
}
