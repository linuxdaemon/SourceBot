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

package net.walterbarnes.sourcebot.common.crash;

import net.walterbarnes.sourcebot.common.reference.Constants;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings ({"WeakerAccess", "SameParameterValue"})
public class CrashReport
{
	private static final Logger logger = Logger.getLogger(CrashReport.class.getName());
	/**
	 * Description of the crash report.
	 */
	private final String description;
	/**
	 * The {@link Throwable} that is the "cause" for this crash and Crash Report.
	 */
	private final Throwable cause;
	private final CrashReportCategory rootCategory = new CrashReportCategory("System Details");
	private final StackTraceElement[] stacktrace = new StackTraceElement[0];
	/**
	 * File of crash report.
	 */
	private File crashReportFile;

	public CrashReport(String description, Throwable cause)
	{
		this.description = description;
		this.cause = cause;
		this.populateEnvironment();
	}

	private void populateEnvironment()
	{
		this.rootCategory.addCrashSectionCallable("SourceBot Version", new GetVersion());
		this.rootCategory.addCrashSectionCallable("Operating System", new GetOSVersion());
		this.rootCategory.addCrashSectionCallable("Java Version", new GetJavaVersion());
		this.rootCategory.addCrashSectionCallable("Java VM Version", new GetJVMVersion());
		this.rootCategory.addCrashSectionCallable("Memory", new GetMemory());
		this.rootCategory.addCrashSectionCallable("JVM Flags", new GetJVMFlags());
	}

	/**
	 * Displays a crash report and saves it to a file
	 *
	 * @param crashReport Report to display
	 */
	public static void displayCrashReport(CrashReport crashReport)
	{
		File file1 = new File(".", "crash-reports");
		File file2 = new File(file1, "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss"))
				.format(new Date()) + ".txt");
		System.out.println(crashReport.getCompleteReport());

		if (crashReport.getFile() != null)
		{
			System.out.println("#@!@# Bot crashed! Crash report saved to: #@!@# " + crashReport.getFile());
		}
		else if (crashReport.saveToFile(file2))
		{
			System.out.println("#@!@# Bot crashed! Crash report saved to: #@!@# " + file2.getAbsolutePath());
		}
		else
		{
			System.out.println("#@?@# Bot crashed! Crash report could not be saved. #@?@#");
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
		stringbuilder.append("\n\nA detailed walk through of the error, its code path and all known details is as follows:\n");

		for (int i = 0; i < 87; ++i)
		{
			stringbuilder.append("-");
		}

		stringbuilder.append("\n\n");
		this.getSectionsInStringBuilder(stringbuilder);
		return stringbuilder.toString();
	}

	public File getFile()
	{
		return this.crashReportFile;
	}

	public boolean saveToFile(File file)
	{
		if (this.crashReportFile != null)
		{
			return false;
		}
		else
		{
			OutputStreamWriter writer = null;
			try
			{
				if (file.getParentFile() != null)
				{
					if (!file.exists() && !file.getParentFile().mkdirs())
					{
						throw new IOException("Unable to create crash-reports directory");
					}
				}

				writer = new OutputStreamWriter(
						new FileOutputStream(file),
						Charset.forName("UTF-8").newEncoder()
				);
				writer.write(this.getCompleteReport());
				writer.close();
				this.crashReportFile = file;
				return true;
			}
			catch (Throwable throwable)
			{
				logger.log(Level.SEVERE, "Could not save crash report to " + file, throwable);
				return false;
			}
			finally
			{
				if (writer != null)
				{
					try
					{
						writer.close();
					}
					catch (IOException e)
					{
						logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
		}
	}

	/**
	 * Gets the stack trace of the {@link Throwable} that caused this crash report, or if that fails, the cause .toString().
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

		//String s = object.toString();

		String s;
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
	 * Gets the various sections of the crash report into the given {@link StringBuilder}
	 */
	public void getSectionsInStringBuilder(StringBuilder stringBuilder)
	{
		if (this.stacktrace.length > 0)
		{
			stringBuilder.append("-- Head --\n");
			stringBuilder.append("Stacktrace:\n");

			for (StackTraceElement stacktraceelement : this.stacktrace)
			{
				stringBuilder.append("\t").append("at ").append(stacktraceelement.toString());
				stringBuilder.append("\n");
			}

			stringBuilder.append("\n");
		}

		this.rootCategory.appendToStringBuilder(stringBuilder);
	}

	private static class GetJVMFlags implements Callable
	{
		public String call()
		{
			RuntimeMXBean runtimemxbean = ManagementFactory.getRuntimeMXBean();
			List list = runtimemxbean.getInputArguments();
			int i = 0;
			StringBuilder stringbuilder = new StringBuilder();

			for (Object aList : list)
			{
				String s = (String) aList;

				if (s.startsWith("-X"))
				{
					if (i++ > 0)
					{
						stringbuilder.append(" ");
					}

					stringbuilder.append(s);
				}
			}

			return String.format("%d total; %s", i, stringbuilder.toString());
		}
	}

	private static class GetMemory implements Callable
	{
		public String call()
		{
			Runtime runtime = Runtime.getRuntime();
			long i = runtime.maxMemory();
			long j = runtime.totalMemory();
			long k = runtime.freeMemory();
			long l = i / 1024L / 1024L;
			long i1 = j / 1024L / 1024L;
			long j1 = k / 1024L / 1024L;
			return k + " bytes (" + j1 + " MB) / " + j + " bytes (" + i1 + " MB) up to " + i + " bytes (" + l + " MB)";
		}
	}

	private static class GetJVMVersion implements Callable
	{
		public String call()
		{
			return System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.info") + "), " + System.getProperty("java.vm.vendor");
		}
	}

	private static class GetJavaVersion implements Callable
	{
		public String call()
		{
			return System.getProperty("java.version") + ", " + System.getProperty("java.vendor");
		}
	}

	private static class GetOSVersion implements Callable
	{
		public String call()
		{
			return System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") version " + System.getProperty("os.version");
		}
	}

	private static class GetVersion implements Callable
	{
		public String call()
		{
			return Constants.getVersion();
		}
	}
}