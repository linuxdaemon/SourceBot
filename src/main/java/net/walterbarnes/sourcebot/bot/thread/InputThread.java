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

package net.walterbarnes.sourcebot.bot.thread;

import net.walterbarnes.sourcebot.bot.SourceBot;
import net.walterbarnes.sourcebot.bot.command.CommandHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class InputThread implements Runnable
{
	@Override
	public void run()
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF-8").newDecoder()));
		CommandHandler ch = SourceBot.INSTANCE.getCommandHandler();
		ch.init();
		String input;
		do
		{
			try
			{
				while (!br.ready())
				{
					Thread.sleep(200);
				}
				input = br.readLine();
				if (input != null && !input.isEmpty())
				{
					ch.addPendingCommand(input);
					ch.executePendingCommands();
				}
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				break;
			}
			catch (IOException ignored) {}
		}
		while (true);
	}
}
