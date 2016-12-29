package com.github.endercrypt.ytddb.backend.app;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Function;

import com.github.endercrypt.ytddb.backend.Backend;
import com.github.endercrypt.ytddb.backend.DataCenter;
import com.github.endercrypt.ytddb.connection.ConnectionListener;
import com.github.endercrypt.ytddb.gui.GuiFrame;

public class BackendApp implements Runnable
{
	private LinkedList<Entry> graph = new LinkedList<>();

	public Entry entry = new Entry();

	private int ceiling = 0;

	private GuiFrame guiFrame = new GuiFrame(new GuiFrame.Listener()
	{
		private final int pointDistance = 8;
		private Dimension screenSize;

		private double getPosition(int position)
		{
			return ((double) screenSize.height) / ceiling * position;
		}

		private void drawGraphLine(Graphics2D g2d, Color color, Function<Entry, Integer> valueFunction)
		{
			g2d.setColor(color);
			int index = -1;
			int lastY = 0;
			Iterator<Entry> iterator = graph.iterator();
			while (iterator.hasNext())
			{
				index++;
				Entry entry = iterator.next();
				int x = screenSize.width - (index * pointDistance);
				int y = screenSize.height - (int) getPosition(valueFunction.apply(entry));
				g2d.drawLine(x + pointDistance, lastY, x, y);
				lastY = y;
			}
		}

		public int getLength(FontMetrics metrics, String... values)
		{
			int length = 0;
			for (String value : values)
			{
				length = Math.max(length, metrics.stringWidth(value));
			}
			return length;
		}

		@Override
		public void onDraw(Graphics2D g2d, Dimension screenSize)
		{
			this.screenSize = screenSize;
			FontMetrics metrics = g2d.getFontMetrics();

			synchronized (graph)
			{
				//g2d.drawString(String.valueOf(ceiling), screenSize.width - 32, 20);
				//g2d.drawString(String.valueOf(0), screenSize.width - 32, screenSize.height - 20);

				int points = screenSize.width / pointDistance + 1;

				while (graph.size() > points)
				{
					graph.removeLast();
				}

				if (graph.size() > 0)
				{
					Entry current = graph.getFirst();
					drawGraphLine(g2d, Color.LIGHT_GRAY, (entry) -> entry.newReceivedRelatedVideos);
					drawGraphLine(g2d, Color.RED, (entry) -> entry.receivedVideoData);

					String stringVideoData = "Video Data received: " + current.receivedVideoData;
					String stringNewVideoIds = "New video id's discovered: " + current.newReceivedRelatedVideos;
					String stringConnections = "Clients connected: " + Backend.connections.size();
					String stringState = "State: " + (DataCenter.isReady() ? "Ready" : "Setting up...");
					int infoLength = getLength(metrics, stringVideoData, stringNewVideoIds, stringConnections, stringState);
					g2d.setColor(Color.BLACK);
					g2d.drawRect(5, 5, infoLength + 5, 59);
					g2d.setColor(Color.WHITE);
					g2d.fillRect(5, 5, infoLength + 5, 59);
					g2d.setColor(Color.BLACK);
					g2d.drawString(stringVideoData, 7, 18);
					g2d.drawString(stringNewVideoIds, 7, 32);
					g2d.drawString(stringConnections, 7, 46);
					g2d.drawString(stringState, 7, 60);
				}
			}
		}
	});

	private void verifyGraph()
	{
		ceiling = 0;
		for (Entry entry : graph)
		{
			ceiling = Math.max(ceiling, entry.getMax());
		}
		ceiling += 10;
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				printData();
				synchronized (graph)
				{
					graph.addFirst(entry);
					entry = new Entry();
					verifyGraph();
				}
				guiFrame.repaint();
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				// ignore
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
		System.out.println("Received data about " + entry.receivedVideoData + " videos");
		int percentage = (int) Math.round(100.0 / (entry.newReceivedRelatedVideos + entry.oldReceivedRelatedVideos) * entry.newReceivedRelatedVideos);
		int expansionRate = (int) Math.round((100.0 / entry.receivedVideoData) * entry.newReceivedRelatedVideos);
		System.out.println("Received " + entry.newReceivedRelatedVideos + " new videos ( and " + entry.oldReceivedRelatedVideos + " already known ones ) " + percentage + "% (growth stability: " + expansionRate + "%)");

	}

	public static class Entry
	{
		public int receivedVideoData = 0;
		public int newReceivedRelatedVideos = 0;
		public int oldReceivedRelatedVideos = 0;

		public int getMax()
		{
			return Math.max(newReceivedRelatedVideos, receivedVideoData);
		}
	}
}
