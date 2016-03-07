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
		ArrayList<String> tags = new ArrayList<>(Arrays.asList(Config.getTags()));

		for (int i = 0; i < tags.size(); i++)
		{
			long startTime = System.currentTimeMillis();
			String tag = tags.get(i);
			System.out.print(tag + " ");
			int x = 0;
			loop:
			for (Post post : tumblr.getPostsFromTag(tag, "text", 100, null))
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

				for (String t : post.getTags())
				{
					if (isTagSimilar(tag.toLowerCase(), t.toLowerCase()) && !tags.contains(t.toLowerCase()))
					{
						tags.add(t.toLowerCase());
					}
				}
				x++;
			}
			System.out.println(x + " posts " + ((System.currentTimeMillis() - startTime) / 1000) + " s");
		}

		Map<String, Integer> tagMap = new HashMap<>();

		for (long postID : postData.keySet())
		{
			for (String t : posts.get(postID).getTags())
			{
				if (!tagMap.containsKey(t))
				{
					tagMap.put(t, 0);
				}
				tagMap.put(t, tagMap.get(t) + 1);
			}
		}
		System.out.println(MapHelper.sortByValues((HashMap<String, Integer>) tagMap));
	}

	private static boolean isTagSimilar(String tag, String tag1)
	{
		//Tags are case-insensitive, so this code is redundant
		//if (tag1.toLowerCase().equals(tag2.toLowerCase())) return true;
		if (tag.contains(tag1) && tag.length() + 1 >= tag1.length() &&
				tag1.length() >= tag.length() - 1 && !tag.equals(tag1))
			return true;
		if (tag1.contains(tag) && tag1.length() + 1 >= tag.length() &&
				tag.length() >= tag1.length() - 1 && !tag1.equals(tag))
			return true;
		return false;
	}
}
