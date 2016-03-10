package net.walterbarnes.sourcebot.util;

import net.walterbarnes.sourcebot.SourceBot;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogHelper
{
	private static final String logFileName = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date()) + "-SourceBot.log";
	private static final Logger logger = Logger.getLogger(SourceBot.class.getName());

	private static FileHandler fileTxt;
	private static SimpleFormatter formatterTxt;

	public static void init()
	{
		try
		{
			fileTxt = new FileHandler(logFileName);
			formatterTxt = new SimpleFormatter();
			logger.setLevel(Level.ALL);
			fileTxt.setFormatter(formatterTxt);
			logger.addHandler(fileTxt);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static Logger getLogger()
	{
		return logger;
	}
}