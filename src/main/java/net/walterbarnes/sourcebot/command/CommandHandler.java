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

package net.walterbarnes.sourcebot.command;

import java.util.*;

public class CommandHandler
{
	private final List<String> commandQueue = Collections.synchronizedList(new ArrayList<String>());
	private final Map<String, ICommand> commandMap = new HashMap<>();

	public void init()
	{
		registerCommand("stop", new CommandStop());
		registerCommand("purge", new CommandPurge());
	}

	public boolean registerCommand(String identifier, ICommand cmd)
	{
		if (!commandMap.keySet().contains(identifier))
		{
			commandMap.put(identifier, cmd);
			return true;
		}
		return false;
	}

	public void addPendingCommand(String command)
	{
		commandQueue.add(command);
	}

	public void executePendingCommands()
	{
		while (!this.commandQueue.isEmpty())
		{
			String cmd = this.commandQueue.remove(0);
			this.executeCommand(cmd);
		}
	}

	private void executeCommand(String input)
	{
		List<String> inputSplit = new ArrayList<>(Arrays.asList(input.split(" ")));
		String cmd = inputSplit.remove(0);
		commandMap.get(cmd).run(inputSplit.toArray(new String[0]));
	}
}
