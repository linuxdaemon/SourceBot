package net.walterbarnes.sourcebot;

import com.tumblr.jumblr.types.Post;
import net.ofd.oflib.map.MapHelper;
import net.walterbarnes.sourcebot.config.Config;
import net.walterbarnes.sourcebot.tumblr.Tumblr;

import java.util.*;

public class Analysis
{
	public static void main(String[] args)
	{
		Tumblr tumblr = new Tumblr(Config.getConsumerKey(), Config.getConsumerSecret(),
				Config.getToken(), Config.getTokenSecret());

		Map<Long, Post> posts = new HashMap<>();
		Map<Long, List<String>> postData = new HashMap<>();

		for (String tag : Config.getTags())
		{
			loop:
			for (Post post : tumblr.getPostsFromTag(tag, null, 10000, null))
			{
				for (String tb : Config.getTagBlacklist())
				{
					if (post.getTags().contains(tb) ||
							Arrays.asList(Config.getBlogBlacklist()).contains(post.getBlogName()))
					{
						continue loop;
					}
				}
				posts.put(post.getId(), post);
				if (!postData.containsKey(post.getId()) || postData.get(post.getId()) == null)
				{
					postData.put(post.getId(), new ArrayList<String>());
				}
				List<String> tagList = postData.get(post.getId());
				tagList.add(tag);
				postData.put(post.getId(), tagList);
			}
		}

		Map<String, Integer> tags = new HashMap<>();

		for (long postID : postData.keySet())
		{
			for (String tag : posts.get(postID).getTags())
			{
				if (!tags.containsKey(tag))
				{
					tags.put(tag, 0);
				}
				tags.put(tag, tags.get(tag) + 1);
			}
		}
		System.out.println(MapHelper.sortByValues((HashMap<String, Integer>) tags));
	}
}
