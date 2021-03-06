package com.github.endercrypt.ytddb.connection;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.github.endercrypt.ytddb.exception.BadWebpageException;
import com.github.endercrypt.ytddb.exception.DownloadFailedException;
import com.github.endercrypt.ytddb.exception.VideoNotAvailable;
import com.github.endercrypt.ytddb.net.NETP_ConnectionException;
import com.github.endercrypt.ytddb.net.NETP_RemoveID;
import com.github.endercrypt.ytddb.net.NETP_VideoData;
import com.github.endercrypt.ytddb.net.NETP_Videos;

public class Client extends ConnectionListener
{
	private static String DEFAULT_USERAGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36";
	private static String YOUTUBE_URL = "https://www.youtube.com/watch?v=";

	private App app;
	private Thread appThread;

	private LinkedList<String> videoIDs = new LinkedList<>();

	public Client(Socket socket) throws IOException
	{
		super(socket);
		setObjectListener(NETP_Videos.class, new ReceivedListener<NETP_Videos>()
		{
			@Override
			public void onReceive(NETP_Videos object)
			{
				List<String> newIDs = Arrays.asList(object.videos);
				videoIDs.addAll(newIDs);
				System.out.println("Received " + newIDs.size() + " new video ID's to download");
			}
		});
		setObjectListener(NETP_ConnectionException.class, new ReceivedListener<NETP_ConnectionException>()
		{
			@Override
			public void onReceive(NETP_ConnectionException object) throws IOException
			{
				System.err.println("An exception occured in the server, which forced this connection to close");
				System.err.println("Please report this error to Magnus/EnderCrypt");
				object.exception.printStackTrace();
				close();
			}
		});
	}

	@Override
	public Thread startListening()
	{
		Thread thread = super.startListening();
		app = new App();
		appThread = new Thread(app);
		appThread.start();
		return thread;
	}

	public class App implements Runnable
	{
		@Override
		public void run()
		{
			boolean videoRequestAllowed = true;
			try
			{
				while (!Thread.interrupted())
				{
					// request new id's if needed
					if (videoIDs.size() <= 5)
					{
						if (videoRequestAllowed == true)
						{
							send(new NETP_Videos(10));
							videoRequestAllowed = false;
							Thread.sleep(1000);
						}
						else
						{
							System.out.println("Failed to request new video id's, as internet seems unstable?");
							Thread.sleep(10000);
						}
					}
					if (videoIDs.size() > 5)
					{
						// download video data
						String videoID = videoIDs.removeFirst();
						try
						{
							processVideo(videoID);
							videoRequestAllowed = true;
						}
						catch (DownloadFailedException e)
						{
							System.err.println("Failed to download " + videoID + " (internet connection lost?)");
							videoIDs.addFirst(videoID);
							Thread.sleep(5000);
						}
					}

				}
			}
			catch (

			InterruptedException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				panic(e);
			}
		}

		private void processVideo(String videoID) throws IOException
		{
			String url = YOUTUBE_URL + videoID;
			Connection connection = Jsoup.connect(url);
			connection.timeout(5000);
			connection.userAgent(DEFAULT_USERAGENT);

			// download document
			Document document = null;
			try
			{
				document = connection.get();
			}
			catch (IOException e)
			{
				throw new DownloadFailedException();
			}

			// parse document into video data
			NETP_VideoData videoData = null;
			try
			{
				videoData = new NETP_VideoData(videoID, document);
			}
			catch (VideoNotAvailable e)
			{
				// page status error
				System.err.println("YT status error: " + e.getMessage());
				send(new NETP_RemoveID(videoID));
				return;
			}
			catch (BadWebpageException e)
			{
				// error trying to parse the page, ignore
				System.err.println("Failed to parse page: reason: \"" + e.getMessage() + "\"");
				send(new NETP_RemoveID(videoID));
				return;
			}
			System.out.println("[" + videoID + "] " + videoData.getTitle());
			send(videoData);
			String[] relatedVideos = getRelatedVideos(document);
			send(new NETP_Videos(relatedVideos));
			return;
		}

		private String[] getRelatedVideos(Document document)
		{
			Set<String> videoIDs = new HashSet<>();
			Elements relatedVideos = document.getElementsByClass("related-list-item-compact-video");
			for (Element relatedVideo : relatedVideos)
			{
				String videoID = relatedVideo.child(1).child(0).child(0).attr("data-vid");
				videoIDs.add(videoID);
			}
			return videoIDs.toArray(new String[videoIDs.size()]);
		}
	}
}
