package net.walterbarnes.sourcebot;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import net.walterbarnes.sourcebot.crash.CrashReport;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;
import net.walterbarnes.sourcebot.tumblr.Tumblr;
import net.walterbarnes.sourcebot.util.LogHelper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SourceBot
{
	private static final String jsonName = "SourceBot.json";
	private static final String postsName = "posts.json";
	private static Logger logger = LogHelper.getLogger();
	private static Gson gson = new Gson();
	private static Gson gsonBuilder = new GsonBuilder().setPrettyPrinting().create();
	private static JsonParser parser = new JsonParser();
	private static JsonObject json, posts;
	private static File jsonFile;
	private static File postsFile;

	public static void main(String[] args)
	{
		CrashReport crashreport;
		try
		{
			LogHelper.init();
			if (!(jsonFile = new File(jsonName)).exists())
			{
				jsonFile.createNewFile();
			}
			if (!(postsFile = new File(postsName)).exists())
			{
				postsFile.createNewFile();
				FileWriter fw = new FileWriter(postsFile);
				JsonWriter pjw = new JsonWriter(fw);
				pjw.beginObject();
				pjw.endObject();
				pjw.close();
				fw.close();
			}
			json = parser.parse(new FileReader(jsonFile)).getAsJsonObject();
			posts = parser.parse(new FileReader(postsFile)).getAsJsonObject();
			run();
		}
		catch (Throwable throwable)
		{
			crashreport = new CrashReport("Unexpected error", throwable);
			displayCrashReport(crashreport);
		}
	}

	public static void run() throws InvalidBlogNameException
	{
		JsonObject api = json.getAsJsonObject("api");
		JsonObject consumer = api.getAsJsonObject("consumer");
		JsonObject token = api.getAsJsonObject("token");
		Tumblr client = new Tumblr(consumer.get("key").getAsString(), consumer.get("secret").getAsString(),
				token.get("key").getAsString(), token.get("secret").getAsString(), logger);

		while (true)
		{
			for (JsonElement blog : json.getAsJsonArray("blogs"))
			{
				JsonObject j = blog.getAsJsonObject();
				String url = j.get("url").getAsString();
				BotThread bt = new BotThread(client.setBlogName(url), j, posts.getAsJsonArray(url));
				logger.info("Running Thread for " + url);
				bt.run();
			}
			try
			{
				FileWriter pjw = new FileWriter(postsFile);
				pjw.append(gsonBuilder.toJson(posts));
				pjw.close();

				FileWriter cjw = new FileWriter(jsonFile);
				cjw.append(gsonBuilder.toJson(json));
				cjw.close();
			}
			catch (IOException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
				System.exit(1);
			}
		}
	}

	public static void displayCrashReport(CrashReport crashReport)
	{
		File file1 = new File(".", "crash-reports");
		File file2 = new File(file1, "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + ".txt");
		System.out.println(crashReport.getCompleteReport());

		if (crashReport.getFile() != null)
		{
			System.out.println("#@!@# Bot crashed! Crash report saved to: #@!@# " + crashReport.getFile());
		}
		else if (crashReport.saveToFile(file2))
		{
			System.out.println("#@!@# Bot crashed! Crash report saved to: #@!@# " + file2.getAbsolutePath());
		}
		else
		{
			System.out.println("#@?@# Bot crashed! Crash report could not be saved. #@?@#");
		}
	}
}
