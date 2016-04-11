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

package net.walterbarnes.sourcebot.cli;

import net.walterbarnes.sourcebot.Install;
import net.walterbarnes.sourcebot.SourceBot;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Cli
{
	private static final Logger logger = Logger.getLogger(Cli.class.getName());
	private String[] args = null;
	private Options options = new Options();

	public Cli(String[] args)
	{
		this.args = args;

		options.addOption(null, "install", false, "Initial install");
		options.addOption("c", "config", true, "Path to config file (Default: ~/.sourcebot)");
	}

	public void parse()
	{
		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		try
		{
			cmd = parser.parse(options, args);

			if (cmd.hasOption('c'))
			{
				setConfigDir(cmd.getOptionValue('c'));
			}
			if (cmd.hasOption("install"))
			{
				install();
			}
		}
		catch (ParseException e)
		{
			e.printStackTrace();
		}
	}

	private void setConfigDir(String dir)
	{
		SourceBot.getCurrentBot().confDir = new File(dir);
		logger.info(String.format("Set config dir to %s", SourceBot.getCurrentBot().confDir.getAbsolutePath()));
	}

	private void install()
	{
		try
		{
			Install.install(SourceBot.getCurrentBot().confDir.getAbsolutePath(), SourceBot.getCurrentBot().confName);
			System.exit(0);
		}
		catch (IOException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			System.exit(1);
		}
	}
}
