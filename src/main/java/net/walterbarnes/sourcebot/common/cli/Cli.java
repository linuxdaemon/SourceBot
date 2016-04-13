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

package net.walterbarnes.sourcebot.common.cli;

import net.walterbarnes.sourcebot.bot.Install;
import net.walterbarnes.sourcebot.bot.SourceBot;
import org.apache.commons.cli.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.logging.Logger;

public class Cli
{
	private static final Logger logger = Logger.getLogger(Cli.class.getName());
	private final Options options = new Options();
	private String[] args = null;

	public Cli(@Nonnull String[] args)
	{
		this.args = args.clone();

		options.addOption(null, "install", false, "Initial install");
		options.addOption("c", "config", true, "Path to config file (Default: ~/.sourcebot)");
	}

	public void parse()
	{
		CommandLineParser parser = new DefaultParser();

		CommandLine cmd;
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

	private void setConfigDir(@Nonnull String dir)
	{
		SourceBot.getCurrentBot().confDir = new File(dir);
		logger.info(String.format("Set config dir to %s", SourceBot.getCurrentBot().confDir.getAbsolutePath()));
	}

	private void install()
	{
		Install.install(SourceBot.getCurrentBot().confDir.getAbsolutePath(), SourceBot.getCurrentBot().confName);
	}
}
