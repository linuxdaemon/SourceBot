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

package net.walterbarnes.sourcebot.thread;

import net.walterbarnes.sourcebot.SourceBot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class InputThread implements Runnable
{
	private static final Logger logger = Logger.getLogger(InputThread.class.getName());

	@Override
	public void run()
	{
		BufferedReader br = new BufferedReader(
				new InputStreamReader(System.in));
		String input = "";
		do
		{
			//System.out.println("> ");
			try
			{
				while (!br.ready())
				{
					Thread.sleep(200);
				}
				input = br.readLine();
			}
			catch (InterruptedException ignored)
			{
				Thread.currentThread().interrupt();
				break;
			}
			catch (IOException ignored) {}
		}
		while (!"stop".equals(input));

		logger.info("Shutting down....");
		SourceBot.running = false;
		if (SourceBot.currentThread != null)
		{
			SourceBot.currentThread.interrupt();
		}
	}
}
