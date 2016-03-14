package net.walterbarnes.sourcebot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.AnswerPost;
import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;
import net.walterbarnes.sourcebot.tumblr.Tumblr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BotThread implements Runnable
{
	private static final Logger logger = Logger.getLogger(SourceBot.class.getName());
	private final Tumblr client;
	private final JsonObject json;
	private final JsonArray seenPosts;

	public BotThread(Tumblr client, JsonObject json, JsonArray posts)
	{
		this.client = client;
		this.json = json;
		this.seenPosts = posts;
	}

	public BotThread(Tumblr client, String blogName, JsonObject json, JsonArray posts) throws InvalidBlogNameException
	{
		this.client = client.setBlogName(blogName);
		this.json = json;
		this.seenPosts = posts;
	}

	private static ArrayList<Post> selectPosts(ArrayList<Post> posts, int n, boolean unique)
	{
		ArrayList<Post> out = new ArrayList<>();

		while (out.size() < n)
		{
			Post post = posts.get(ThreadLocalRandom.current().nextInt(0, posts.size() + 1));
			out.add(post);
			if (unique) posts.remove(post);
		}
		return out;
	}

	private static ArrayList<Post> getTopPosts(ArrayList<Post> posts)
	{
		int moves = 0;
		boolean firstRun = true;
		while (firstRun || moves > 0)
		{
			moves = 0;
			firstRun = false;
			for (int i = 1; i < posts.size(); i++)
			{
				Post a = posts.get(i - 1);
				Post b = posts.get(i);

				if (a.getNoteCount() < b.getNoteCount())
				{
					posts.set(i - 1, b);
					posts.set(i, a);
					moves++;
				}
			}
		}
		return new ArrayList<>(posts.subList(0, 49));
	}

	@Override
	public void run()
	{
		JsonArray blogAdminsJson = json.getAsJsonArray("admins");
		List<String> blogAdmins = new ArrayList<>();

		for (JsonElement s : blogAdminsJson)
		{
			blogAdmins.add(s.getAsString());
		}

		JsonObject blacklist = json.getAsJsonObject("blacklist");
		JsonArray tagBlacklist = blacklist.getAsJsonArray("tags");
		JsonArray blogBlacklist = blacklist.getAsJsonArray("blogs");

		JsonObject whitelist = json.getAsJsonObject("whitelist");
		JsonArray tagWhitelist = whitelist.getAsJsonArray("tags");
		JsonArray blogWhitelist = whitelist.getAsJsonArray("blogs");

		if (client.blogSubmissions().size() > 0)
		{
			logger.info("Parsing Submissions");
			List<AnswerPost> asks = client.getAsks();
			for (AnswerPost ask : asks)
			{
				logger.info("Processing ask");
				if (blogAdmins.contains(ask.getAskingName()))
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
													logger.fine("Adding " + words[i] + " to blacklist");
													blogBlacklist.add(new JsonPrimitive(words[i]));
												}
											}
											logger.fine("Deleting ask with id " + ask.getId());
											ask.delete();
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
													logger.fine("Adding " + tag + " to search list");
													tagWhitelist.add(new JsonPrimitive(tag));
												}
											}
											logger.fine("Deleting ask with id " + ask.getId());
											ask.delete();
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
													logger.fine("Adding tag '" + tag + "' to blacklist");
													tagBlacklist.add(new JsonPrimitive(tag));
												}
											}
											logger.fine("Deleting ask with id " + ask.getId());
											ask.delete();
											break;
									}
								}
								break;
						}
					}
				}
			}
		}

		try
		{
			while (client.blogDraftPosts().size() < 20)
			{
				logger.info("Adding posts to queue");
				ArrayList<Post> posts = new ArrayList<>();
				for (JsonElement tag : tagWhitelist)
				{
					posts.addAll(client.getPostsFromTag(tag.getAsString(), json.get("postType").getAsString(),
							1000, null, blogBlacklist, tagBlacklist,
							seenPosts));
				}
				for (Post post : selectPosts(json.get("posts").getAsString().equals("top") ? getTopPosts(posts) : posts, 1, true))
				{
					Map<String, Object> params = new HashMap<>();
					params.put("state", json.get("state").getAsString());
					params.put("comment", json.get("comment").getAsString());
					try
					{
						post.reblog(client.getBlogName(), params);
					}
					catch (NullPointerException e)
					{
						logger.log(Level.SEVERE, e.getMessage(), e);
					}
					seenPosts.add(new JsonPrimitive(post.getId()));
				}
			}
		}
		catch (JumblrException ignored) {}
		catch (Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public JsonObject getJson()
	{
		return json;
	}
}
