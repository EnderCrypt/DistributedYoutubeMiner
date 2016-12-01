package com.github.endercrypt.ytddb.backend;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.github.endercrypt.ytddb.connection.ConnectionListener;
import com.github.endercrypt.ytddb.connection.ReceivedListener;
import com.github.endercrypt.ytddb.net.NETP_ConnectionException;
import com.github.endercrypt.ytddb.net.NETP_RemoveID;
import com.github.endercrypt.ytddb.net.NETP_VideoData;
import com.github.endercrypt.ytddb.net.NETP_Videos;

public class BackendConnection extends ConnectionListener
{
	public BackendConnection(Socket socket) throws IOException
	{
		super(socket);

		setObjectListener(NETP_VideoData.class, new ReceivedListener<NETP_VideoData>()
		{
			@Override
			public void onReceive(NETP_VideoData object)
			{
				DataCenter.addVideo(object);
				Backend.app.receivedVideoData++;
			}
		});

		setObjectListener(NETP_Videos.class, new ReceivedListener<NETP_Videos>()
		{
			@Override
			public void onReceive(NETP_Videos object) throws IOException
			{
				int toSend = object.videoIdRequest;
				if (toSend > 0)
				{
					Set<String> videoIdSet = DataCenter.getVideoIDs(toSend);
					String[] videoIDs = videoIdSet.toArray(new String[videoIdSet.size()]);
					send(new NETP_Videos(videoIDs));
				}
				if (object.videos != null)
				{
					Set<String> videoIDs = new HashSet<>(Arrays.asList(object.videos));
					DataCenter.removeExisting(videoIDs);
					Backend.app.receivedRelatedVideos += videoIDs.size();
					DataCenter.addVideoIDs(videoIDs);
				}
			}
		});

		setObjectListener(NETP_RemoveID.class, new ReceivedListener<NETP_RemoveID>()
		{
			@Override
			public void onReceive(NETP_RemoveID object)
			{
				//DataCenter.remove(object.videoID); IGNORED currently
			}
		});
	}

	@Override
	protected void exceptionOccured(Exception e)
	{
		try
		{
			send(new NETP_ConnectionException(e));
		}
		catch (IOException ie)
		{
			// ignore
		}
	}

	@Override
	public void close() throws IOException
	{
		Backend.connections.remove(this);
		super.close();
	}
}
