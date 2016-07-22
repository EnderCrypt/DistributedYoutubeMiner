package com.github.endercrypt.ytddb.net;

import java.io.Serializable;

public class NETP_ConnectionException implements Serializable
{
	private static final long serialVersionUID = 3249817258178040842L;

	/**
	 * 
	 */

	public Exception exception;

	public NETP_ConnectionException(Exception exception)
	{
		this.exception = exception;
	}
}
