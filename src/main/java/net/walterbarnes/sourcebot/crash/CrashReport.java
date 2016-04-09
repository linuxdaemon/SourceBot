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

import net.walterbarnes.sourcebot.util.LogHelper;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

@SuppressWarnings ({"WeakerAccess", "SameParameterValue"})
public class CrashReport
{
	/**
	 * Description of the crash report.
	 */
	private final String description;
	/**
	 * The Throwable that is the "cause" for this crash and Crash Report.
	 */
	private final Throwable cause;
	private final StackTraceElement[] stacktrace = new StackTraceElement[0];
	/**
	 * File of crash report.
	 */
	private File crashReportFile;

	public CrashReport(String description, Throwable cause)
	{
		this.description = description;
		this.cause = cause;
	}

	public File getFile()
	{
		return this.crashReportFile;
	}

	public boolean saveToFile(File p_147149_1_)
	{
		if (this.crashReportFile != null)
		{
			return false;
		}
		else
		{
			if (p_147149_1_.getParentFile() != null)
			{
				//noinspection ResultOfMethodCallIgnored
				p_147149_1_.getParentFile().mkdirs();
			}

			try
			{
				FileWriter filewriter = new FileWriter(p_147149_1_);
				filewriter.write(this.getCompleteReport());
				filewriter.close();
				this.crashReportFile = p_147149_1_;
				return true;
			}
			catch (Throwable throwable)
			{
				LogHelper.getLogger().log(Level.SEVERE, "Could not save crash report to " + p_147149_1_, throwable);
				return false;
			}
		}
	}

	/**
	 * Gets the complete report with headers, stack trace, and different sections as a string.
	 */
	public String getCompleteReport()
	{
		StringBuilder stringbuilder = new StringBuilder();
		stringbuilder.append("---- SourceBot Crash Report ----\n");
		stringbuilder.append("\n\n");
		stringbuilder.append("Time: ");
		stringbuilder.append((new SimpleDateFormat()).format(new Date()));
		stringbuilder.append("\n");
		stringbuilder.append("Description: ");
		stringbuilder.append(this.description);
		stringbuilder.append("\n\n");
		stringbuilder.append(this.getCauseStackTraceOrString());
		stringbuilder.append("\n\nA detailed walkthrough of the error, its code path and all known details is as follows:\n");

		for (int i = 0; i < 87; ++i)
		{
			stringbuilder.append("-");
		}

		stringbuilder.append("\n\n");
		this.getSectionsInStringBuilder(stringbuilder);
		return stringbuilder.toString();
	}

	/**
	 * Gets the stack trace of the Throwable that caused this crash report, or if that fails, the cause .toString().
	 */
	public String getCauseStackTraceOrString()
	{
		StringWriter stringwriter = null;
		PrintWriter printwriter = null;
		Object object = this.cause;

		if (((Throwable) object).getMessage() == null)
		{
			if (object instanceof NullPointerException)
			{
				object = new NullPointerException(this.description);
			}
			else if (object instanceof StackOverflowError)
			{
				object = new StackOverflowError(this.description);
			}
			else if (object instanceof OutOfMemoryError)
			{
				object = new OutOfMemoryError(this.description);
			}

			((Throwable) object).setStackTrace(this.cause.getStackTrace());
		}

		String s = object.toString();

		try
		{
			stringwriter = new StringWriter();
			printwriter = new PrintWriter(stringwriter);
			((Throwable) object).printStackTrace(printwriter);
			s = stringwriter.toString();
		}
		finally
		{
			try
			{
				if (stringwriter != null)
				{
					stringwriter.close();
				}
			}
			catch (IOException ioe)
			{
				// ignore
			}

			if (printwriter != null)
			{
				printwriter.close();
			}
		}

		return s;
	}

	/**
	 * Gets the various sections of the crash report into the given StringBuilder
	 */
	public void getSectionsInStringBuilder(StringBuilder stringBuilder)
	{
		if (this.stacktrace.length > 0)
		{
			stringBuilder.append("-- Head --\n");
			stringBuilder.append("Stacktrace:\n");
			StackTraceElement[] astacktraceelement = this.stacktrace;

			for (StackTraceElement stacktraceelement : astacktraceelement)
			{
				stringBuilder.append("\t").append("at ").append(stacktraceelement.toString());
				stringBuilder.append("\n");
			}

			stringBuilder.append("\n");
		}
	}
}