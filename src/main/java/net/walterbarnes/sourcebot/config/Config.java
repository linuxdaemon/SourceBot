package net.walterbarnes.sourcebot.config;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Config
{
	public static Config config;
	private String cfgName;
	private BufferedReader cfgReader;
	private BufferedWriter cfgWriter;
	private LinkedHashMap<String, String> data = new LinkedHashMap<>();

	public Config ()
	{
		this ("./SourceBot.cfg");
	}

	public Config (String file)
	{
		cfgName = file;
		config = this;
		load();
	}

	public static void load()
	{
		if (config == null) config = new Config();
		config.parse();
	}

	public void parse()
	{
		try
		{
			cfgReader = new BufferedReader (new FileReader(new File(cfgName)));
			String line;
			loop:
			while ((line = cfgReader.readLine ()) != null)
			{
				String l = "";
				for (Character c : line.toCharArray ())
				{
					if (c.equals ('#'))
					{
						data.put("comment","#" + line.split("#")[1]);
						break;
					}
					l += c;
				}
				if (l.isEmpty () || l.equals (""))
				{
					data.put(null,null);
					continue;
				}
				if (l.matches ("[A-Za-z0-9]+=[^$]+"))
				{
					data.put (l.split ("=")[0], l.split ("=")[1]);
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public Object get(String key)
	{
		return config.data.get (key);
	}

	public static String getConsumerKey ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return (String) config.get ("consumerKey");
	}

	public static String getConsumerSecret ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return (String) config.get ("consumerSecret");
	}

	public static String getToken ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return (String) config.get ("token");
	}

	public static String getTokenSecret ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return (String) config.get ("tokenSecret");
	}

	public static String getBlogUrl ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return (String) config.get ("blogUrl");
	}

	public static int getPostFreq ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return Integer.parseInt ((String) config.get ("postFrequency"));
	}

	public static String[] getTags ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return ((String)config.get ("tags")).split (",");
	}

	public static String[] getTagBlacklist ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return ((String)config.get ("tagBlacklist")).split (",");
	}

	public static String[] getBlogBlacklist ()
	{
		if (config == null)
		{
			config = new Config ();
		}
		if (config.get ("blogBlacklist") == null)
		{
			return new String[]{};
		}
		else
		{
			return ((String)config.get ("blogBlacklist")).split (",");
		}
	}

	public static List<Long> getPostBlacklist()
	{
		if (config == null)
		{
			config = new Config();
		}
		List<Long> out = new ArrayList<>();
		if (config.get("postBlacklist") == null)
		{
			return new ArrayList<>();
		}
		for (String post : ((String)config.get("postBlacklist")).split(","))
		{
			out.add(Long.getLong(post));
		}
		return out;
	}

	public static void setPostBlacklist(List<Long> blacklist)
	{
		String s = "";
		for (Long l : blacklist)
		{
			s += l + ",";
		}
		config.data.put("postBlacklist", s.substring(0, s.length()-1));
		save();
	}

	public static void save()
	{
		if (config == null)
		{
			config = new Config();
		}
		config.write();
	}

	public void write()
	{
		try
		{
			Thread.sleep(50);
			cfgWriter = new BufferedWriter(new FileWriter(new File(cfgName)));
			for (Map.Entry<String, String> e : data.entrySet())
			{
				if (e.getKey().equals("comment"))
				{
					cfgWriter.write(e.getValue());
					continue;
				}
				if (e.getKey() == null && e.getValue() == null)
				{
					cfgWriter.write("\n");
					continue;
				}
				cfgWriter.write(String.format("%s=%s%n", e.getKey(), e.getValue()));
			}
			cfgWriter.flush();
			Thread.sleep(50);
			parse();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
}
