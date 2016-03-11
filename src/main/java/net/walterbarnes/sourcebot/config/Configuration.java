package net.walterbarnes.sourcebot.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

class Configuration
{

	private static final String NEW_LINE;

	static
	{
		NEW_LINE = System.getProperty("line.separator");
	}

	File file;
	private String fileName = null;
	private boolean changed = false;

	public Configuration() {}

	public Configuration(File file)
	{
		this.file = file;
		String path = file.getAbsolutePath().replace("/./", "/");
		fileName = path;
		try
		{
			load();
		}
		catch (Throwable e)
		{
			File fileBak = new File(file.getAbsolutePath() + "_" +
					new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".errored");
			System.err.println(String.format("An exception occurred while loading config file %s. This file will be " +
				"renamed to %s and a new config file will be generated.", file.getName(), fileBak.getName()));
			e.printStackTrace();

			file.renameTo(fileBak);
			load();
		}
	}

	@Override
	public String toString() { return file.getAbsolutePath(); }

	public void load()
	{
		BufferedReader buffer = null;
		FileReader input = null;
		try
		{
			if (file.getParentFile() != null)
			{
				file.getParentFile().mkdirs();
			}

			if (!file.exists())
			{
				if (!file.createNewFile())
				{ return; }
			}

			if (file.canRead())
			{
				input = new FileReader(file);
				buffer = new BufferedReader(input);

				String line;
				while (true)
				{
					break;
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (buffer != null)
			{
				try
				{
					buffer.close();
				}
				catch (IOException ignored) {}
			}
			if (input != null)
			{
				try
				{
					input.close();
				}
				catch (IOException ignored) {}
			}
		}
	}

}
