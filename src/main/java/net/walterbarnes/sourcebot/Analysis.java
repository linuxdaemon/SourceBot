package net.walterbarnes.sourcebot;

import com.tumblr.jumblr.types.AnswerPost;
import com.tumblr.jumblr.types.Post;
import com.tumblr.jumblr.types.TextPost;
import net.ofd.oflib.map.MapHelper;
import net.walterbarnes.sourcebot.config.Config;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;
import net.walterbarnes.sourcebot.tumblr.Tumblr;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;

public class Analysis
{
	public static void main(String[] args)
	{
		Tumblr tumblr = new Tumblr(Config.getConsumerKey(), Config.getConsumerSecret(),
				Config.getToken(), Config.getTokenSecret());

		try
		{
			tumblr.setBlogName("thelinuxdemon");
		}
		catch (InvalidBlogNameException e)
		{
			e.printStackTrace();
		}
		//System.out.println(tumblr.rawBlogSubmissions(tumblr.getBlogName()).getBody().replace(",", ",\n"));
		//System.exit(0);

		List<Post> subs;
		if ((subs = tumblr.blogSubmissions(tumblr.getBlogName())).size() > 0)
		{
			Post post = subs.get(0);
			System.out.println("submission received");
			//System.out.println(((AnswerPost)post).getAskingName());
			if (((AnswerPost)post).getAskingName().equals("thelinuxdemon"))
			{
				System.out.println(((AnswerPost)post).getAskingName());
				System.out.println(post.getState());
				System.out.println(((AnswerPost) post).getQuestion());
				try
				{
					Map<String, Object> detail = new HashMap<>();
					detail.put("state", "private");
					detail.put("tags", post.getTags() == null ? "" : StringUtils.join(post.getTags().toArray(new String[0]), ","));
					detail.put("format", post.getFormat());
					detail.put("slug", post.getSlug());
					detail.put("date", post.getDateGMT());
					detail.put("answer", "test response");
					detail.put("blog_name", ((AnswerPost)post).getAskingName());
					tumblr.postEdit(tumblr.getBlogName(), post.getId(), detail);
					//if (post.getId() == null) {
					//	post.setId(tumblr.postCreate(tumblr.getBlogName(), detail));
					//} else {
					//	tumblr.postEdit(tumblr.getBlogName(), post.getId(), detail);
					//}
					System.out.println(post.getState());
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			//post.reblog(tumblr.getBlogName(), opts);
		}
		System.exit(0);
		Map<Long, Post> posts = new HashMap<>();
		Map<Long, List<String>> postData = new HashMap<>();
		ArrayList<String> tags = new ArrayList<>(Arrays.asList(Config.getTags()));

		for (int i = 0; i < tags.size(); i++)
		{
			long startTime = System.currentTimeMillis();
			String tag = tags.get(i);
			//System.out.print(tag + " ");
			int x = 0;
			HashMap<String, Object> options = new HashMap<> ();
			options.put("filter","text");
			loop:
			for (Post post : tumblr.getPostsFromTag(tag, "text", 100, options))
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
			//System.out.println(x + " posts " + ((System.currentTimeMillis() - startTime) / 1000) + " s");
		}

		Map<String, Integer> wordMap = new HashMap<>();

		for (long postID : postData.keySet())
		{
			for (String word : ((TextPost)posts.get(postID)).getBody().split("[ \n,]"))
			{
				word = stripPunct(word);
				if (!wordMap.containsKey(word))
				{
					wordMap.put(word, 0);
				}
				wordMap.put(word, wordMap.get(word) + 1);
			}
		}
		System.out.println(MapHelper.sortByValues((HashMap<String, Integer>) wordMap));
	}

	private static boolean isTagSimilar(String tag, String tag1)
	{
		int d = levenshteinDistance(tag, tag1);
		return false;
	}

	public static int levenshteinDistance(CharSequence lhs, CharSequence rhs) {
		int len0 = lhs.length() + 1;
		int len1 = rhs.length() + 1;

		// the array of distances
		int[] cost = new int[len0];
		int[] newcost = new int[len0];

		// initial cost of skipping prefix in String s0
		for (int i = 0; i < len0; i++) cost[i] = i;

		// dynamically computing the array of distances

		// transformation cost for each letter in s1
		for (int j = 1; j < len1; j++) {
			// initial cost of skipping prefix in String s1
			newcost[0] = j;

			// transformation cost for each letter in s0
			for(int i = 1; i < len0; i++) {
				// matching current letters in both strings
				int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;

				// computing cost for each transformation
				int cost_replace = cost[i - 1] + match;
				int cost_insert  = cost[i] + 1;
				int cost_delete  = newcost[i - 1] + 1;

				// keep minimum cost
				newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
			}

			// swap cost/newcost arrays
			int[] swap = cost; cost = newcost; newcost = swap;
		}

		// the distance is the cost for transforming all letters in both strings
		return cost[len0 - 1];
	}

	private static String stripPunct(String s)
	{
		ArrayList<String> approvedChar = new ArrayList<>();
		approvedChar.addAll(Arrays.asList("abcdefghijklmnopqrstuvwxyz".split("")));
		approvedChar.addAll(Collections.singletonList(" "));
		String out = "";
		for (String c : s.split(""))
		{
			if (approvedChar.contains(c)) out += c;
		}
		return out;
	}
}
