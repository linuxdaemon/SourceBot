package net.walterbarnes.sourcebot;

import com.tumblr.jumblr.types.Post;
import net.ofd.oflib.map.MapHelper;
import net.walterbarnes.sourcebot.config.Config;
import net.walterbarnes.sourcebot.tumblr.Tumblr;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Analysis
{
	public static void main(String[] args)
	{
		Tumblr tumblr = new Tumblr(Config.getConsumerKey(), Config.getConsumerSecret(),
				Config.getToken(), Config.getTokenSecret());

		HashMap<String, HashMap<String, Integer>> relatedTags = new HashMap<>();

		for (String tag : Config.getTags())
		{
			HashMap<String, Integer> tags = new HashMap<>();
			for (Post post : tumblr.getPostsFromTag(tag, null, 10000, null))
			{
				for (String t : post.getTags())
				{
					if (!t.toLowerCase().equals(tag))
					{
						if (tags.containsKey(t.toLowerCase()))
						{
							tags.put(t.toLowerCase(), tags.get(t.toLowerCase()) + 1);
						}
						else
						{
							tags.put(t.toLowerCase(), 1);
						}
					}
				}
			}
			LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
			LinkedHashMap<String, Integer> sorted = MapHelper.sortByValues(tags);

			for (int i = 0; i < 5; i++)
			{
				out.put(((Map.Entry<String, Integer>) (sorted.entrySet().toArray()[i])).getKey(), ((Map.Entry<String, Integer>) (sorted.entrySet().toArray()[i])).getValue());
			}
			relatedTags.put(tag, MapHelper.sortByValues(out));
		}
		System.out.println(relatedTags);
	}
}
