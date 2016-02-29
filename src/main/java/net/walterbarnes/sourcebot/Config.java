package net.walterbarnes.sourcebot;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Config
{
	private static Config config;
	private Map<String, String> data = new HashMap<> ();

	public Config ()
	{
		this ("./SourceBot.cfg");
	}

	public Config (String file)
	{
		try
		{
			File f = new File (file);
			FileReader fr = new FileReader (f);
			BufferedReader br = new BufferedReader (fr);
			String line;
			while ((line = br.readLine ()) != null)
			{
				String l = "";
				for (Character c : line.toCharArray ())
				{
					if (c.equals ('#'))
					{
						break;
					}
					l += c;
				}
				if (l.isEmpty () || l.equals (""))
				{
					continue;
				}
				if (l.matches ("[A-Za-z0-9]+=[^$]+"))
				{
					data.put (l.split ("=")[0], l.split ("=")[1]);
				}
			}
		} catch (FileNotFoundException e)
		{
			e.printStackTrace ();
		} catch (IOException e)
		{
			e.printStackTrace ();
		}
	}

	public static Object get(String key)
	{
		if (config == null)
		{
			config = new Config ();
		}
		return config.data.get (key);
	}

	public static String getConsumerKey ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return config.data.get ("consumerKey");
	}

	public static String getConsumerSecret ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return config.data.get ("consumerSecret");
	}

	public static String getToken ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return config.data.get ("token");
	}

	public static String getTokenSecret ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return config.data.get ("tokenSecret");
	}

	public static String getBlogUrl ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return config.data.get ("blogUrl");
	}

	public static int getPostFreq ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return Integer.parseInt (config.data.get ("postFrequency"));
	}

	public static String[] getTags ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return config.data.get ("tags").split (",");
	}

	public static String[] getTagBlacklist ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return config.data.get ("tagBlacklist").split (",");
	}

	public static String[] getBlogBlacklist ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		if (config.data.get ("blogBlacklist") == null)
		{
			return new String[]{};
		}
		else
		{
			return config.data.get ("blogBlacklist").split (",");
		}
	}
}
