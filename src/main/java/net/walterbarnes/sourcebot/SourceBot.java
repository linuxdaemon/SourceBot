package net.walterbarnes.sourcebot;

import com.tumblr.jumblr.types.Post;
import net.ofd.oflib.cli.Cli;
import net.walterbarnes.sourcebot.config.Config;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;
import net.walterbarnes.sourcebot.tumblr.Tumblr;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SourceBot
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

		long time = System.currentTimeMillis();
		while (true)
		{
			if (client.blogDraftPosts(client.getBlogName()).size() < 24)
			{
				Config.load();
				int postCount = 0;
				System.out.println("Adding posts to queue");
				while (postCount < 5)
				{
					ArrayList<Post> posts = new ArrayList<> ();
					for (String tag : Config.getTags ())
					{
						Cli.print("Getting posts for tag '" + tag + "'");
						posts.addAll (client.getPostsFromTag (tag, "text", 10, null, Arrays.asList(Config.getBlogBlacklist()), Arrays.asList(Config.getTagBlacklist()), Config.getPostBlacklist()));
					}
					for (Post post : selectPosts (getTopPosts (posts), 1))
					{
						Map<String, Object> params = new HashMap<> ();
						params.put("state", "draft");
						params.put ("comment", "Source?");
						try
						{
							//post.reblog(client.getBlogName(), params);
						} catch (NullPointerException e)
						{
							e.printStackTrace ();
						}
						List<Long> pbl = Config.getPostBlacklist();
						pbl.add(post.getId());
						Config.setPostBlacklist(pbl);
					}
					postCount++;
				}
			}
			try
			{
				Thread.sleep(10000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	private static ArrayList<Post> selectPosts (ArrayList<Post> posts, int n)
	{
		ArrayList<Post> out = new ArrayList<> ();

		while (out.size () < n)
		{
			Post post = posts.get (ThreadLocalRandom.current ().nextInt (0, posts.size () + 1));
			out.add (post);
			posts.remove (post);
		}
		return out;
	}

	private static ArrayList<Post> getTopPosts (ArrayList<Post> posts)
	{
		ArrayList<Post> out = (ArrayList<Post>) posts.clone ();
		int moves = 0;
		boolean firstRun = true;
		while (firstRun || moves > 0)
		{
			moves = 0;
			firstRun = false;
			for (int i = 1; i < out.size (); i++)
			{
				Post a = out.get (i - 1);
				Post b = out.get (i);

				if (a.getNoteCount () < b.getNoteCount ())
				{
					out.set (i - 1, b);
					out.set (i, a);
					moves++;
				}
			}
		}

		return new ArrayList<> (out.subList (0, 49));
	}
}
