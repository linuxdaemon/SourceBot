package net.walterbarnes.sourcebot;

import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.config.Config;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;
import net.walterbarnes.sourcebot.tumblr.Tumblr;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SourceBot
{
	public static void main (String[] args)
	{
		// TODO Allow config from asks from blog admins
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

		long time = System.currentTimeMillis();
		while (true)
		{
			if (client.blogDraftPosts().size() < 10)
			{
				Config.load();
				int postCount = 0;
				System.out.println("Adding posts to queue");
				while (postCount < 5)
				{
					ArrayList<Post> posts = new ArrayList<> ();
					for (String tag : Config.getTags ())
					{
						System.out.println("Searching tag " + tag);
						posts.addAll (client.getPostsFromTag (tag, "text",
								args.length > 0 && args[0].equals("debug") ? 10 : 1000, null,
								Arrays.asList(Config.getBlogBlacklist()), Arrays.asList(Config.getTagBlacklist()),
								Config.getPostBlacklist()));
					}
					for (Post post : selectPosts (getTopPosts (posts), 1))
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
