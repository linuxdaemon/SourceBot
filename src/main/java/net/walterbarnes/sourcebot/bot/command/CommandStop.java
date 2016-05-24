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

package net.walterbarnes.sourcebot.bot.command;

import net.walterbarnes.sourcebot.bot.SourceBot;
import net.walterbarnes.sourcebot.bot.util.LogHelper;

import javax.annotation.Nullable;

public class CommandStop implements ICommand
{
	@Override
	public void run(@Nullable String[] args)
	{
		LogHelper.info("Shutting down....");

		SourceBot.INSTANCE.running = false;
		if (SourceBot.INSTANCE.currentThread != null)
		{
			SourceBot.INSTANCE.currentThread.interrupt();
		}
	}
}
