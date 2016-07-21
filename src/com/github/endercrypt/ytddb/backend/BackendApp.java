package com.github.endercrypt.ytddb.backend;

import com.github.endercrypt.ytddb.connection.ConnectionListener;

public class BackendApp implements Runnable
{
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
		System.out.println("Received data about " + Backend.receivedVideoData + " videos");
		Backend.receivedVideoData = 0;
		System.out.println("Received " + Backend.receivedRelatedVideos + " related videos");
		Backend.receivedRelatedVideos = 0;
	}
}
