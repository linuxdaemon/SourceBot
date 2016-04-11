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

package net.walterbarnes.sourcebot.crash;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class CrashReportCategory
{
	private final CrashReport crashReport;
	private final String categoryName;
	private final List<CrashReportCategory.Entry> sections = new ArrayList<>();
	private StackTraceElement[] stackTrace = new StackTraceElement[0];

	public CrashReportCategory(CrashReport crashReport, String categoryName)
	{
		this.crashReport = crashReport;
		this.categoryName = categoryName;
	}

	/**
	 * Adds a Crashreport section with the given name with the value set to the result of the given Callable;
	 */
	public void addCrashSectionCallable(String title, Callable callable)
	{
		try
		{
			this.addCrashSection(title, callable.call());
		}
		catch (Throwable throwable)
		{
			this.addCrashSectionThrowable(title, throwable);
		}
	}

	/**
	 * Adds a Crashreport section with the given name with the given value (convered .toString())
	 */
	public void addCrashSection(String title, Object content)
	{
		this.sections.add(new CrashReportCategory.Entry(title, content));
	}

	/**
	 * Adds a Crashreport section with the given name with the given Throwable
	 */
	public void addCrashSectionThrowable(String title, Throwable throwable)
	{
		this.addCrashSection(title, throwable);
	}

	/**
	 * Resets our stack trace according to the current trace, pruning the deepest 3 entries.  The parameter indicates
	 * how many additional deepest entries to prune.  Returns the number of entries in the resulting pruned stack trace.
	 */
	public int getPrunedStackTrace(int trimLength)
	{
		StackTraceElement[] astacktraceelement = Thread.currentThread().getStackTrace();

		if (astacktraceelement.length <= 0)
		{
			return 0;
		}
		else
		{
			int len = astacktraceelement.length - 3 - trimLength;
			if (len <= 0) len = astacktraceelement.length;
			this.stackTrace = new StackTraceElement[len];
			System.arraycopy(astacktraceelement, astacktraceelement.length - len, this.stackTrace, 0, this.stackTrace.length);
			return this.stackTrace.length;
		}
	}

	/**
	 * Do the deepest two elements of our saved stack trace match the given elements, in order from the deepest?
	 */
	public boolean firstTwoElementsOfStackTraceMatch(StackTraceElement stackTraceElement, StackTraceElement stackTraceElement1)
	{
		if (this.stackTrace.length != 0 && stackTraceElement != null)
		{
			StackTraceElement stacktraceelement2 = this.stackTrace[0];

			if (stacktraceelement2.isNativeMethod() == stackTraceElement.isNativeMethod() && stacktraceelement2.getClassName().equals(stackTraceElement.getClassName()) && stacktraceelement2.getFileName().equals(stackTraceElement.getFileName()) && stacktraceelement2.getMethodName().equals(stackTraceElement.getMethodName()))
			{
				if (stackTraceElement1 == null || !(this.stackTrace.length > 1))
				{
					return false;
				}
				else if (!this.stackTrace[1].equals(stackTraceElement1))
				{
					return false;
				}
				else
				{
					this.stackTrace[0] = stackTraceElement;
					return true;
				}
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}

	/**
	 * Removes the given number entries from the bottom of the stack trace.
	 */
	public void trimStackTraceEntriesFromBottom(int n)
	{
		StackTraceElement[] astacktraceelement = new StackTraceElement[this.stackTrace.length - n];
		System.arraycopy(this.stackTrace, 0, astacktraceelement, 0, astacktraceelement.length);
		this.stackTrace = astacktraceelement;
	}

	public void appendToStringBuilder(StringBuilder stringBuilder)
	{
		stringBuilder.append("-- ").append(this.categoryName).append(" --\n");
		stringBuilder.append("Details:");

		for (Entry entry : this.sections)
		{
			stringBuilder.append("\n\t");
			stringBuilder.append(entry.getTitle());
			stringBuilder.append(": ");
			stringBuilder.append(entry.getContent());
		}

		if (this.stackTrace != null && this.stackTrace.length > 0)
		{
			stringBuilder.append("\nStacktrace:");
			StackTraceElement[] stackTraceElements = this.stackTrace;
			int j = stackTraceElements.length;

			for (int i = 0; i < j; ++i)
			{
				StackTraceElement stackTraceElement = stackTraceElements[i];
				stringBuilder.append("\n\tat ");
				stringBuilder.append(stackTraceElement.toString());
			}
		}
	}

	public StackTraceElement[] getStackTrace()
	{
		return this.stackTrace;
	}

	static class Entry
	{
		private final String title;
		private final String content;

		public Entry(String title, Object content)
		{
			this.title = title;

			if (content == null)
			{
				this.content = "~~NULL~~";
			}
			else if (content instanceof Throwable)
			{
				Throwable throwable = (Throwable) content;
				this.content = "~~ERROR~~ " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
			}
			else
			{
				this.content = content.toString();
			}
		}

		public String getTitle()
		{
			return this.title;
		}

		public String getContent()
		{
			return this.content;
		}
	}
}