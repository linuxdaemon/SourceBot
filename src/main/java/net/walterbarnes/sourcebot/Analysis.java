package net.walterbarnes.sourcebot;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.Blog;
import com.tumblr.jumblr.types.Post;
import com.tumblr.jumblr.types.TextPost;
import com.tumblr.jumblr.types.User;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Analysis
{

	public static void main (String[] args)
	{
		// Authenticate via OAuth
		JumblrClient client = new JumblrClient (
				Config.getConsumerKey (),
				Config.getConsumerSecret ()
		);
		client.setToken (
				Config.getToken (),
				Config.getTokenSecret ()
		);

		String blogName = Config.getBlogUrl ();

		// Make the request
		User user = client.user ();
		ArrayList<Blog> blogs = (ArrayList<Blog>) user.getBlogs ();
		ArrayList<String> blogNames = new ArrayList<> ();
		for (Blog b : blogs)
		{
			blogNames.add (b.getName ());
		}
		if (!blogNames.contains (blogName))
		{
			System.out.println ("Invalid Blog Url");
			System.exit (1);
		}

		File postsDir = new File ("posts");
		if (!postsDir.exists ())
			postsDir.mkdirs ();

		for (String tag : Config.getTags ())
		{
			File tagDir = new File (postsDir, tag);
			if (!tagDir.exists ())
				tagDir.mkdirs ();
			int postCount = 0;
			long lastTime = System.currentTimeMillis () / 1000;
			while (postCount < 1000000)
			{
				HashMap<String, Object> options = new HashMap<> ();
				options.put ("before", lastTime);
				List<Post> posts = client.tagged (tag, options);
				if (posts.size () == 0 || posts.isEmpty ())
					break;
				for (Post post : posts)
				{
					if (post.getType ().equals ("text"))
					{
						try
						{
							TextPost textPost = (TextPost) post;
							BufferedWriter bw = new BufferedWriter (new FileWriter (new File (tagDir, textPost.getId ().toString ())));
							bw.write ("Blog:" + textPost.getBlogName () + "\n");
							bw.write ("Title:" + textPost.getTitle () + "\n");
							bw.write ("Body:" + textPost.getBody () + "\n");
							bw.close ();
							postCount++;
						} catch (IOException e)
						{
							e.printStackTrace ();
						}
					}
					lastTime = post.getTimestamp ();
				}
			}
		}
//		while (postCount < 1)
//		{
//			ArrayList<Post> posts = new ArrayList<> ();
//			for (String tag : Config.getTags ())
//			{
//				posts.addAll (getPosts (client, tag));
//			}
//			for (Post post : selectPosts (getTopPosts (posts), 1))
//			{
//				System.out.println (post.getPostUrl ());
//				System.out.println (post.getNoteCount ());
//			}
//			Map<String, Object> params = new HashMap<> ();
//			params.put ("comment", "Source?");
//			try
//			{
//				//post.reblog(blogName, params);
//			} catch (NullPointerException e)
//			{
//				e.printStackTrace ();
//			}
//			postCount++;
//			try
//			{
//				System.out.println ("sleeping");
//				Thread.sleep (Config.getPostFreq () * 60 * 1000);
//			} catch (InterruptedException e)
//			{
//				e.printStackTrace ();
//				System.exit (1);
//			}
//		}
	}

	private static ArrayList<Post> selectPosts (ArrayList<Post> posts, int n)
	{
		System.out.println ("selectPosts");
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
		System.out.println ("getTopPosts");
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
			//System.out.println(moves);
		}

		return new ArrayList<> (out.subList (0, 9));
	}

	private static ArrayList<Post> getPosts (JumblrClient client, String tag)
	{
		System.out.println ("getPosts " + tag);
		ArrayList<Post> posts = new ArrayList<Post> ();
		Long lastTime = System.currentTimeMillis () / 1000;
		loop:
		while (posts.size () < 10000)
		{
			ArrayList<Post> p = new ArrayList<> ();
			HashMap<String, Object> options = new HashMap<> ();
			options.put ("before", lastTime);
			//System.out.println("Getting tag posts " + posts.size ());
			p.addAll (client.tagged (tag, options));
			if (p.size () == 0 || p.isEmpty ())
			{
				break loop;
			}
			else
			{
				lastTime = p.get (p.size () - 1).getTimestamp ();
			}
			for (Post post : p)
			{
				for (String tb : Config.getTagBlacklist ())
				{
					if (post.getType ().equals ("text") && !post.getTags ().contains (tb) &&
							!(new ArrayList<String> (Arrays.asList (Config.getBlogBlacklist ())))
									.contains (post.getBlogName ()))
					{
						posts.add (post);
					}
				}
			}
			//System.out.println(posts.size ());
		}
		return posts;
	}
}
