package net.walterbarnes.sourcebot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.tumblr.jumblr.types.Post;
import com.tumblr.jumblr.types.TextPost;
import net.walterbarnes.sourcebot.crash.CrashReport;
import net.walterbarnes.sourcebot.exception.InvalidBlogNameException;
import net.walterbarnes.sourcebot.tumblr.Tumblr;
import net.walterbarnes.sourcebot.util.LogHelper;
import org.scribe.exceptions.OAuthConnectionException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SourceBot
{
	private static final String jsonName = "SourceBot.json";
	private static Logger logger = Logger.getLogger(SourceBot.class.getName());
	private static JsonParser parser = new JsonParser();
	private static JsonObject json;
	private static File jsonFile;
	private static Connection conn;
	private static String[] args;

	public static void main(String[] args)
	{
		SourceBot.args = args;
		CrashReport crashreport;
		try
		{
			LogHelper.init(SourceBot.class);
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			if (!(jsonFile = new File(jsonName)).exists())
			{
				if (!jsonFile.createNewFile())
				{
					throw new RuntimeException("Unable to Create Config File");
				}
				FileWriter fw = new FileWriter(jsonFile);
				JsonWriter pjw = new JsonWriter(fw);
				pjw.beginObject();
				pjw.endObject();
				pjw.close();
				fw.close();
			}
			json = parser.parse(new FileReader(jsonFile)).getAsJsonObject();
			run();
		}
		catch (Throwable throwable)
		{
			crashreport = new CrashReport("Unexpected error", throwable);
			displayCrashReport(crashreport);
		}
	}

	public static void run() throws InvalidBlogNameException, SQLException, FileNotFoundException,
			InstantiationException, IllegalAccessException
	{
		JsonObject api = json.getAsJsonObject("api");
		JsonObject consumer = api.getAsJsonObject("consumer");
		JsonObject token = api.getAsJsonObject("token");
		JsonObject db = json.getAsJsonObject("db");
		Tumblr client = new Tumblr(consumer.get("key").getAsString(), consumer.get("secret").getAsString(),
				token.get("key").getAsString(), token.get("secret").getAsString(), logger);
		conn = DriverManager.getConnection("jdbc:mysql://" + db.get("host").getAsString() + "/" +
				db.get("db_name").getAsString(), db.get("user").getAsString(), db.get("pass").getAsString());

		if (args.length == 2)
		{
			if (args[0].equals("getPostIds"))
			{
				client.setBlogName(args[1]);
				List<Post> posts = client.getBlogPosts();
				for (Post post : posts)
				{
					System.out.print(post.getId() + " ");
				}
				System.out.println();
				System.exit(0);
			}
			else
			{
				JsonObject postJson = parser.parse(new FileReader(args[0])).getAsJsonObject();
				TextPost tp = client.newPost(args[1], TextPost.class);
				tp.setTitle(postJson.get("title").getAsString());
				tp.setBody(postJson.get("body").getAsString());
				tp.setState("draft");
				tp.save();
				System.exit(0);
			}
		}
		PreparedStatement getBlogs = conn.prepareStatement("SELECT DISTINCT url FROM blogs ORDER BY id;");
		while (true)
		{
			try
			{
				ResultSet rs = getBlogs.executeQuery();
				while (rs.next())
				{
					String url = rs.getString("url");
					BotThread bt = new BotThread(client, url, conn);
					logger.info("Running Thread for " + url);
					bt.run();
				}
			}
			catch (OAuthConnectionException e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	public static void displayCrashReport(CrashReport crashReport)
	{
		File file1 = new File(".", "crash-reports");
		File file2 = new File(file1, "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss"))
				.format(new Date()) + ".txt");
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
