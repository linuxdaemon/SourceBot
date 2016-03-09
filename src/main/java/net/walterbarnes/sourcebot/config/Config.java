package net.walterbarnes.sourcebot.config;

import java.io.*;
import java.util.*;

public class Config
{
	private static Config config;
	private final String cfgName;
	private BufferedReader cfgReader;
	private BufferedWriter cfgWriter;
	private LinkedHashMap<String, String> data = new LinkedHashMap<>();

	private Config()
	{
		this ("./SourceBot.cfg");
	}

	private Config(String file)
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

	public static String getConsumerKey()
	{
		if (config == null)
		{
			config = new Config();
		}
		return (String) config.get("consumerKey");
	}

	public static String getConsumerSecret()
	{
		if (config == null)
		{
			config = new Config();
		}
		return (String) config.get("consumerSecret");
	}

	public static String getToken()
	{
		if (config == null)
		{
			config = new Config();
		}
		return (String) config.get("token");
	}

	public static String getTokenSecret()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return (String) config.get("tokenSecret");
	}

	public static String getBlogUrl()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return (String) config.get("blogUrl");
	}

	public static String[] getBlogAdmins()
	{
		if (config == null)
		{
			config = new Config();
		}
		return ((String) config.get("blogAdmins")).split(",");
	}

	public static int getPostFreq()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return Integer.parseInt((String) config.get("postFrequency"));
	}

	public static String[] getTags()
	{
		if (config == null)
		{
			config = new Config();
		}
		return ((String) config.get("tags")).split(",");
	}

	public static List<String> getTagBlacklist()
	{
		if (config == null)
		{
			config = new Config ();
		}
		return new ArrayList<>(Arrays.asList(((String) config.get("tagBlacklist")).split(",")));
	}

	public static void setTagBlacklist(List<String> blacklist)
	{
		String s = "";
		for (String blog : blacklist)
		{
			s += blog + ",";
		}
		config.data.put("tagBlacklist", s.substring(0, s.length() - 1));
		save();
	}

	public static List<String> getBlogBlacklist()
	{
		if (config == null)
		{
			config = new Config ();
		}
		if (config.get("blogBlacklist") == null)
		{
			return new ArrayList<>();
		}
		else
		{
			return new ArrayList<>(Arrays.asList(((String) config.get("blogBlacklist")).split(",")));
		}
	}

	public static void setBlogBlacklist(List<String> blacklist)
	{
		String s = "";
		for (String blog : blacklist)
		{
			s += blog + ",";
		}
		config.data.put("blogBlacklist", s.substring(0, s.length() - 1));
		save();
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
		for (String postID : ((String)config.get("postBlacklist")).split(","))
		{
			out.add(Long.valueOf(postID));
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

	private static void save()
	{
		if (config == null)
		{
			config = new Config();
		}
		config.write();
	}

	private void parse()
	{
		try
		{
			cfgReader = new BufferedReader(new FileReader(new File(cfgName)));
			String line;
			while ((line = cfgReader.readLine()) != null)
			{
				String l = "";
				for (Character c : line.toCharArray())
				{
					if (c.equals('#'))
					{
						data.put("comment", "#" + line.split("#")[1]);
						break;
					}
					else
					{
						l += c;
					}
				}
				if (l.isEmpty() || l.equals(""))
				{
					data.put("newLine", null);
				}
				else if (l.matches("[A-Za-z0-9]+=[^$]+"))
				{
					data.put(l.split("=")[0], l.split("=")[1]);
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private Object get(String key)
	{
		return config.data.get(key);
	}

	private void write()
	{
		if (config == null)
		{
			config = new Config();
		}
		try
		{
			Thread.sleep(50);
			cfgWriter = new BufferedWriter(new FileWriter(new File(cfgName)));
			for (Map.Entry<String, String> e : data.entrySet())
			{
				if (e.getKey().equals("newLine"))
				{
					cfgWriter.write("\n");
				}
				else if (e.getKey().equals("comment"))
				{
					cfgWriter.write(e.getValue() + "\n");
				}
				else { cfgWriter.write(String.format("%s=%s%n", e.getKey(), e.getValue())); }
			}
			cfgWriter.flush();
			Thread.sleep(50);
			parse();
		}
		catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
		}
	}
}
