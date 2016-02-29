package net.walterbarnes.sourcebot;

import com.tumblr.jumblr.types.Post;
import com.tumblr.jumblr.types.TextPost;
import net.walterbarnes.sourcebot.config.Config;
import net.walterbarnes.sourcebot.tumblr.Tumblr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Analysis
{
	public static void main (String[] args)
	{
		File postsDir = new File ("posts");
		if (!postsDir.exists ())
			postsDir.mkdirs ();

		Tumblr tumblr = new Tumblr (Config.getConsumerKey (), Config.getConsumerSecret (),
				Config.getToken (), Config.getTokenSecret ());

		for (String tag : Config.getTags ())
		{
			File tagDir = new File (postsDir, tag);
			if (!tagDir.exists ())
				tagDir.mkdirs ();
			for (Post post : tumblr.getPostsFromTag (tag, "text", 10000, null))
			{
				try
				{
					TextPost textPost = (TextPost) post;
					BufferedWriter bw = new BufferedWriter (new FileWriter (new File (tagDir, textPost.getId ().toString ())));
					bw.write ("Blog:" + textPost.getBlogName () + "\n");
					bw.write ("Title:" + textPost.getTitle () + "\n");
					bw.write ("Body:" + textPost.getBody () + "\n");
					bw.close ();
				}
				catch (IOException e)
				{
					e.printStackTrace ();
				}
			}
		}
	}
}
