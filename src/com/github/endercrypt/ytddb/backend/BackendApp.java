package com.github.endercrypt.ytddb.backend;

import com.github.endercrypt.ytddb.connection.ConnectionListener;

public class BackendApp implements Runnable
{
	public int receivedVideoData = 0;
	public int receivedRelatedVideos = 0;

	@Override
	public void run()
	{
		while (true)
		{
			printData();
			try
			{
				Thread.sleep(10_000);
			}
			catch (InterruptedException e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	private void printData()
	{
		System.out.println();
		// List users
		StringBuilder sb = new StringBuilder();
		sb.append("Connected (" + Backend.connections.size() + ")");
		for (ConnectionListener connection : Backend.connections)
		{
			sb.append(" " + connection.IP);
		}
		System.out.println(sb.toString());
		// List gathered data
		System.out.println("Received data about " + receivedVideoData + " videos");
		receivedVideoData = 0;
		System.out.println("Received " + receivedRelatedVideos + " related videos");
		receivedRelatedVideos = 0;
	}
}
