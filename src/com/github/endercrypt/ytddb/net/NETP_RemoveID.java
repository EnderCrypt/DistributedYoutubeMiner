package com.github.endercrypt.ytddb.net;

import java.io.Serializable;

public class NETP_RemoveID implements Serializable
{
	private static final long serialVersionUID = 5785036156186551993L;

	/**
	 * 
	 */

	public String videoID = null;

	public NETP_RemoveID(String videoID)
	{
		this.videoID = videoID;
	}
}
