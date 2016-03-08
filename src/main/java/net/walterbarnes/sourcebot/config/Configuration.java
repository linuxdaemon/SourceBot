package net.walterbarnes.sourcebot.config;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Configuration
{
	File file;
	private String fileName = null;

	public Configuration() {};

	public Configuration(File file)
	{
		this.file = file;
		String path = file.getAbsolutePath().replace("/./", "/");
		fileName = path;
		try
		{
			//load();
		}
		catch (Throwable e)
		{
			File fileBak = new File(file.getAbsolutePath() + "_" +
					new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".errored");
			System.err.println(String.format("An exception occurred while loading config file %s. This file will be " +
				"renamed to %s and a new config file will be generated.", file.getName(), fileBak.getName()));
			e.printStackTrace();

			file.renameTo(fileBak);
			//load();
		}
	}

	@Override
	public String toString() { return file.getAbsolutePath(); }


}
