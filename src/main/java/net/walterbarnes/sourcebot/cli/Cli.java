package net.walterbarnes.sourcebot.cli;

import java.io.Console;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

@SuppressWarnings ("WeakerAccess")
public class Cli
{
	private static Console console = System.console();

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

	public static String readPassword()
	{
		return console == null ? readLine() : new String(console.readPassword());
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

	public static int promptInt(String title)
	{
		return Integer.parseInt(prompt(title, Pattern.compile("[0-9]+")));
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
}
