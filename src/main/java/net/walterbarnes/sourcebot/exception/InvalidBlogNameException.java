package net.walterbarnes.sourcebot.exception;

public class InvalidBlogNameException extends Exception
{
	private String blogName;

	public InvalidBlogNameException (String blogName)
	{
		this.blogName = blogName;
	}

	public String getBlogName ()
	{
		return blogName;
	}
}
