package net.walterbarnes.sourcebot.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@SuppressWarnings ("FieldCanBeLocal")
public class LogHelper
{
	private static final String logFileName = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date()) +
			"-SourceBot.log";
	private static final String logsDir = "logs";
	private static Logger logger;

	private static FileHandler fileTxt;
	private static SimpleFormatter formatterTxt;

	public static void init(Class c)
	{
		try
		{
			logger = Logger.getLogger(c.getName());
			File dir = new File(logsDir);
			if (!(dir.exists() && dir.isDirectory()))
			{
				//noinspection ResultOfMethodCallIgnored
				dir.mkdirs();
			}
			fileTxt = new FileHandler("logs/" + logFileName);
			formatterTxt = new SimpleFormatter();
			logger.setLevel(Level.ALL);
			fileTxt.setFormatter(formatterTxt);
			logger.addHandler(fileTxt);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static Logger getLogger()
	{
		return logger;
	}
}
