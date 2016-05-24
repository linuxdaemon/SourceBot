/*
 * Copyright (c) 2016.
 * This file is part of SourceBot.
 *
 * SourceBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SourceBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SourceBot.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.walterbarnes.sourcebot.bot.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogHelper
{
	private static final String logFileName = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date()) +
			"-SourceBot.log";
	private static final String logsDir = "logs";
	private static final Logger logger = LogManager.getLogger("net.walterbarnes.sourcebot.bot");

	public static void init()
	{
//		try
//		{
//			File dir = new File(logsDir);
//			if (!(dir.exists() && dir.isDirectory()))
//			{
//				if (!dir.mkdirs())
//				{
//					throw new RuntimeException("Unable to create log dir");
//				}
//			}
//			FileHandler fileTxt = new FileHandler("logs/" + logFileName);
//			SimpleFormatter formatterTxt = new SimpleFormatter();
//			logger.setLevel(Level.ALL);
//
//			fileTxt.setFormatter(formatterTxt);
//			logger.addHandler(fileTxt);
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//			throw new RuntimeException("Unable to start logger");
//		}
	}

	public static void log(Object msg, String level)
	{
		log(msg, Level.getLevel(level));
	}

	private static void log(Object msg, Level level)
	{
		logger.log(level, msg);
	}

	public static void info(Object msg)
	{
		log(msg, Level.INFO);
	}

	public static void debug(Object msg)
	{
		log(msg, Level.DEBUG);
	}

	public static void fatal(Object msg)
	{
		log(msg, Level.FATAL);
	}

	public static void error(Object msg)
	{
		log(msg, Level.ERROR);
	}

	public static void warn(Object msg)
	{
		log(msg, Level.WARN);
	}

	public static void trace(Object msg)
	{
		log(msg, Level.TRACE);
	}
}
