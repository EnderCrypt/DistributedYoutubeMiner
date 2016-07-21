package com.github.endercrypt.ytddb.net;

import java.io.Serializable;

public class NETP_Videos implements Serializable
{
	private static final long serialVersionUID = -4420794308119341070L;

	/**
	 * 
	 */

	public int videoIdRequest = 0;
	public String[] videos = null;

	public NETP_Videos(int videoIdRequest)
	{
		this.videoIdRequest = videoIdRequest;
	}

	public NETP_Videos(String... videoIDs)
	{
		videos = videoIDs;
	}
}
