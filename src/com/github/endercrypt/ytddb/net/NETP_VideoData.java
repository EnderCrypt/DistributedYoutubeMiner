package com.github.endercrypt.ytddb.net;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.github.endercrypt.ytddb.exception.BadWebpageException;
import com.github.endercrypt.ytddb.exception.VideoNotAvailable;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NETP_VideoData implements Serializable
{
	private static final long serialVersionUID = 3684322771401557862L;
	/**
	 * 
	 */
	private String videoID;
	private String title;
	private String description;
	private long views;
	private int likes;
	private int dislikes;
	private boolean ratingsEnabled = true;
	private String uploader;
	private int length;

	public NETP_VideoData(String videoID, Document document) throws BadWebpageException
	{
		//System.out.println(document);
		/*
		if (document.select("#unavailable-message").size() > 0)
		{
			throw new VideoNotAvailable("The received data (from youtube) suggests that this miner may have been temporarily blocked by youtube, try again in 30-60 minutes");
		}
		*/
		/*
		try
		{
			Files.write(Paths.get("output.html"), document.toString().getBytes(), StandardOpenOption.CREATE);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}*/
		this.videoID = videoID;
		this.title = document.select("#eow-title").text();
		Element descriptionElement = document.getElementById("eow-description");
		if (descriptionElement == null)
			throw new BadWebpageException("Couldnt find Video Description");
		description = descriptionElement.text();
		String scriptTag = document.select("#player-mole-container > script:nth-child(4)").html();
		String startMarker = "ytplayer.config = ";
		int startLocation = scriptTag.indexOf(startMarker);
		int endLocation = scriptTag.indexOf(";ytplayer.load = function()");
		if ((startLocation == -1) || (endLocation == -1))
		{
			throw new BadWebpageException("Failed to find meta data in <script> object");
		}
		startLocation = startLocation + startMarker.length();
		String jsonString = scriptTag.substring(startLocation, endLocation);
		JsonObject json = (JsonObject) new JsonParser().parse(jsonString);

		this.views = json.getAsJsonObject("args").getAsJsonPrimitive("view_count").getAsLong();
		try
		{
			this.likes = Integer.parseInt(document.select("#watch8-sentiment-actions > span > span:nth-child(1) > button > span").text().replaceAll("[^0-9.]", ""));
			this.dislikes = Integer.parseInt(document.select("#watch8-sentiment-actions > span > span:nth-child(3) > button > span").text().replaceAll("[^0-9.]", ""));
		}
		catch (NumberFormatException e)
		{
			ratingsEnabled = false;
			// like & dislike is disabled
		}
		this.uploader = document.select("#watch7-user-header > div > a").text();
		this.length = json.getAsJsonObject("args").getAsJsonPrimitive("length_seconds").getAsInt();
	}

	private static String getStringBetween(String from, String start, String end) throws BadWebpageException
	{
		int startIndex = from.indexOf(start);
		if (startIndex == -1)
			throw new BadWebpageException("unable to find start: " + start);
		startIndex += start.length();
		int endindex = from.indexOf(end, startIndex);
		if (endindex == -1)
			throw new BadWebpageException("unable to find end: " + end);
		return from.substring(startIndex, endindex);
	}

	public String getVideoID()
	{
		return videoID;
	}

	public String getTitle()
	{
		return title;
	}

	public String getDescription()
	{
		return description;
	}

	public long getViews()
	{
		return views;
	}

	public boolean isRatingsEnabled()
	{
		return ratingsEnabled;
	}

	public int getLikes()
	{
		return likes;
	}

	public int getDislikes()
	{
		return dislikes;
	}

	public int getLength()
	{
		return length;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public void setVideoID(String videoID)
	{
		this.videoID = videoID;
	}

	public void setViews(long views)
	{
		this.views = views;
	}

	public void setLikes(int likes)
	{
		this.likes = likes;
	}

	public void setDislikes(int dislikes)
	{
		this.dislikes = dislikes;
	}

	public String getUploader()
	{
		return uploader;
	}

	public void setLength(int length)
	{
		this.length = length;
	}

	public double getLikeRatio()
	{
		return 1.0 / (likes + dislikes) * likes;
	}

	public static transient String SQL_STATEMENT = "INSERT INTO VideoData (Title, Description, Channel, Views, Likes, Dislikes, Length) VALUES (?,?,?,?,?,?,?)";

	public void prepareStatement(PreparedStatement statement) throws SQLException
	{
		statement.setString(1, title);
		statement.setString(2, description);
		statement.setString(3, uploader);
		statement.setLong(4, views);
		if (isRatingsEnabled())
		{
			statement.setInt(5, likes);
			statement.setInt(6, dislikes);
		}
		else
		{
			statement.setNull(5, java.sql.Types.INTEGER);
			statement.setNull(6, java.sql.Types.INTEGER);
		}
		statement.setInt(7, length);
	}
}
