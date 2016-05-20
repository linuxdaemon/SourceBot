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

package net.walterbarnes.sourcebot.bot.crash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

@SuppressWarnings ("WeakerAccess")
public class CrashReportCategory
{
	private final String categoryName;
	private final Collection<Entry> sections = new ArrayList<>();

	public CrashReportCategory(@SuppressWarnings ("SameParameterValue") String categoryName)
	{
		this.categoryName = categoryName;
	}

	/**
	 * Adds a {@link CrashReportCategory} section with the given name with the value set to the result of the given {@link Callable};
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
	 * Adds a {@link CrashReport} section with the given name with the given value (converted .toString())
	 *
	 * @param title   Crash section title
	 * @param content Crash section content
	 */
	private void addCrashSection(String title, Object content)
	{
		this.sections.add(new CrashReportCategory.Entry(title, content));
	}

	/**
	 * Adds a {@link CrashReport} section with the given name with the given {@link Throwable}
	 *
	 * @param title     Crash section title
	 * @param throwable Error to report
	 */
	public void addCrashSectionThrowable(String title, Throwable throwable)
	{
		this.addCrashSection(title, throwable);
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