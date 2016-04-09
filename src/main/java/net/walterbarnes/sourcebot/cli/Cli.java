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

import java.io.Console;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

@SuppressWarnings ({"WeakerAccess", "ClassWithoutLogger", "PublicMethodWithoutLogging", "SameParameterValue"})
public class Cli
{
	private static final Console console = System.console();

	public static String prompt(String title, Pattern valid, String def)
	{
		String s = prompt(title, valid);
		if (s.trim().isEmpty())
		{
			return def;
		}
		return s;
	}

	public static String prompt(String title, Pattern valid)
	{
		String l;
		printf(title);
		while (!valid.matcher(l = readLine()).matches())
		{
			System.out.println("Invalid Value!!");
			System.out.print(title);
		}
		return l;
	}

	public static void printf(String format, Object... args)
	{
		if (console == null)
		{ System.out.print(String.format(format, (java.lang.Object[]) args)); }
		else
		{ console.printf(format, (java.lang.Object[]) args); }
	}

	public static String readLine()
	{
		if (console != null)
		{ return console.readLine(); }
		Scanner s = new Scanner(System.in);
		return s.nextLine();
	}

	public static int promptInt(String title, int def)
	{
		String l = prompt(title, Pattern.compile(" ?[0-9]*"));
		return l.isEmpty() || l.equals(" ") ? def : Integer.parseInt(l);
	}

	public static boolean promptYesNo(String title)
	{
		Pattern affirm = Pattern.compile("[Yy ]?");
		String l = prompt(title, Pattern.compile("[YyNn ]?"));
		return affirm.matcher(l).matches();
	}

	public static String promptList(String title, Map<String, String> opts)
	{
		LinkedList<String> l = new LinkedList<>();
		printf("%s%n", title);
		for (Map.Entry<String, String> entry : opts.entrySet())
		{
			l.add(entry.getValue());
			printf("%d) %s%n", l.size(), entry.getKey());
		}
		int i = promptInt("Type?: ");
		while (i > l.size())
		{
			printf("Invalid Option");
			i = promptInt("Type?: ");
		}
		return l.get(i - 1);
	}

	public static int promptInt(String title)
	{
		return Integer.parseInt(prompt(title, Pattern.compile("[0-9]+")));
	}

	public static String password(String title, Pattern valid)
	{
		printf(title);
		String p;
		while (!valid.matcher(p = readPassword()).matches())
		{
			printf("Invalid Value!!%n");
			printf(title);
		}
		return p;
	}

	public static String readPassword()
	{
		return console == null ? readLine() : new String(console.readPassword());
	}
}
