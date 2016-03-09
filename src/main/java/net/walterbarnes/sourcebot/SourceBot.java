package net.walterbarnes.sourcebot;

import com.tumblr.jumblr.types.AnswerPost;
import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.config.Config;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;
import net.walterbarnes.sourcebot.tumblr.Tumblr;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

class SourceBot
{
	public static void main (String[] args)
	{
		Config.load();
		Tumblr client = new Tumblr (Config.getConsumerKey (), Config.getConsumerSecret (),
				Config.getToken (), Config.getTokenSecret ());

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
				for (Post post : drafts)
				{
					post.delete();
				}
			}
			System.exit(0);
		}

		while (true)
		{
			if (client.blogSubmissions().size() > 0)
			{
				List<AnswerPost> asks = client.getAsks();
				for (AnswerPost ask : asks)
				{
					if (Arrays.asList(Config.getBlogAdmins()).contains(ask.getAskingName()))
					{
						String[] words;
						if ((words = ask.getQuestion().split(" ")).length > 0 && words[0].matches("[Cc]onfig") && words.length > 1)
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
													for (int i = 3; i < words.length; i++)
													{
														List<String> bbl = Config.getBlogBlacklist();
														bbl.add(words[i]);
														Config.load();
														Config.setBlogBlacklist(bbl);
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
												System.out.println(ask.getQuestion());
												if (words.length > 3)
												{
													for (String tag : ask.getQuestion().replace("config tagblacklist add ", "").split(","))
													{
														List<String> bbl = Config.getTagBlacklist();
														bbl.add(tag);
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
						System.out.println("Adding posts to queue");
						ArrayList<Post> posts = new ArrayList<> ();
						for (String tag : Config.getTags ())
						{
							System.out.println("Searching tag " + tag);
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
							} catch (NullPointerException ignored) {}
							List<Long> pbl = Config.getPostBlacklist();
							pbl.add(post.getId());
							Config.load();
							Config.setPostBlacklist(pbl);
						}
					}
					Thread.sleep(10000);
				}
				catch (IllegalStateException | InterruptedException e)
				{
					e.printStackTrace();
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
