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

package net.walterbarnes.sourcebot.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogHelper
{
	private static final String logFileName = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date()) +
			"-SourceBot.log";
	private static final String logsDir = "logs";
	private static Logger logger;

	public static void init()
	{
		try
		{
			logger = Logger.getLogger("net.walterbarnes.sourcebot");
			File dir = new File(logsDir);
			if (!(dir.exists() && dir.isDirectory()))
			{
				if (!dir.mkdirs())
				{
					throw new RuntimeException("Unable to create log dir");
				}
			}
			FileHandler fileTxt = new FileHandler("logs/" + logFileName);
			SimpleFormatter formatterTxt = new SimpleFormatter();
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
