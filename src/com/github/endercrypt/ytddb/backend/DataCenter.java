package com.github.endercrypt.ytddb.backend;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.github.endercrypt.ytddb.net.NETP_VideoData;

/**
 * @author EnderCrypt
 *
 */
public class DataCenter
{
	private static final boolean JAR = DataCenter.class.getResource(DataCenter.class.getSimpleName() + ".class").toString().startsWith("rsrc:");

	private static final String START_VIDEO = "zKx2B8WCQuw";

	private static int currentRow;

	private static Connection connection;

	private static boolean ready = false;

	/**
	 * should be activated once, to initialise the connection to the database
	 * @param connection
	 * @throws IOException
	 */
	public static void init(Connection connection) throws IOException
	{
		DataCenter.connection = connection;
		executeSqlScript("createMainTable");
		if (countTotalRows("Videos") == 0)
		{
			addVideoIDs(START_VIDEO);
		}
		currentRow = DataCenter.getCurrentRow() - 1;
		ready = true;
		System.out.println("Current row: " + currentRow);
	}

	public static boolean isReady()
	{
		return ready;
	}

	/**
	 * Attempts to create a BufferedReader that connects to a resource file (a file INSIDE src/)
	 * @param resource
	 * @return
	 * @throws FileNotFoundException
	 */
	public static BufferedReader readResource(String resource) throws FileNotFoundException
	{
		Reader fileReader;
		if (JAR)
		{
			InputStream inputStream = DataCenter.class.getResourceAsStream("/" + resource);
			fileReader = new InputStreamReader(inputStream);
		}
		else
		{
			fileReader = new FileReader("src/" + resource);
		}
		return new BufferedReader(fileReader);
	}

	/**
	 * attempts to execute a script at src/sql/*.sql
	 * @param name
	 */
	protected static void executeSqlScript(String name)
	{
		// get the script
		StringBuilder sqlScriptBuilder = new StringBuilder();
		try (BufferedReader br = readResource("sql/" + name + ".sql"))
		{
			char c;
			while ((c = (char) br.read()) != 65535)
			{
				sqlScriptBuilder.append(c);
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		// execute the script
		try (Statement statement = connection.createStatement())
		{
			statement.executeUpdate(sqlScriptBuilder.toString());
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * get the curret row that the Videos table is on (the lowest id row that doesent have a VideoData)
	 * @return
	 */
	protected static int getCurrentRow()
	{
		try (Statement statement = connection.createStatement())
		{
			try (ResultSet resultSet = statement.executeQuery("SELECT ID FROM Videos WHERE VideoDataID IS NULL LIMIT 1"))
			{
				if (resultSet.next())
				{
					return resultSet.getInt(1);
				}
				throw new RuntimeException();
			}
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * accepts a set of VideoID's and remove the video id's of those who have already been downloaded in the database
	 * @param videoIDs
	 */
	protected static int removeExisting(Set<String> videoIDs)
	{
		int removed = 0;
		try (PreparedStatement statement = connection.prepareStatement("SELECT VideoID FROM Videos WHERE VideoID = ? LIMIT 1"))
		{
			Iterator<String> iter = videoIDs.iterator();
			while (iter.hasNext())
			{
				String videoID = iter.next();
				statement.setString(1, videoID);
				try (ResultSet resultSet = statement.executeQuery())
				{
					if (resultSet.next() == true)
					{
						removed++;
						iter.remove();
					}
				}
			}
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
		return removed;
	}

	/**
	 * counts the amount of rows in a table
	 * @param table
	 * @return
	 */
	protected static int countTotalRows(String table)
	{
		try (Statement statement = connection.createStatement())
		{
			try (ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM " + table))
			{
				if (resultSet.next())
				{
					return resultSet.getInt(1);
				}
				throw new RuntimeException();
			}
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * gets the next available "to download" youtube video that hasnt already been downloaded before
	 * @return
	 */
	protected static Set<String> getVideoIDs(int limit)
	{
		Set<String> videoIDs = new HashSet<>();
		try (PreparedStatement statement = connection.prepareStatement("SELECT ID, VideoID FROM Videos WHERE ID > ? AND VideoDataID IS NULL LIMIT ?"))
		{
			statement.setInt(1, currentRow);
			statement.setInt(2, limit);
			try (ResultSet resultSet = statement.executeQuery())
			{
				while (resultSet.next())
				{
					currentRow = resultSet.getInt(1);
					videoIDs.add(resultSet.getString(2));
				}
			}
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
		return videoIDs;
	}

	/**
	 * same as below (overloaded)
	 * @param videoIDs
	 */
	protected static void addVideoIDs(String... videoIDs)
	{
		addVideoIDs(new HashSet<>(Arrays.asList(videoIDs)));
	}

	/**
	 * attempts to add 1 or more video id's to the database for future download
	 * be sure to remove id's that are already in the database first
	 * @param videoIDs
	 */
	protected static void addVideoIDs(Set<String> videoIDs)
	{
		// java.sql.SQLException: [SQLITE_CONSTRAINT]  Abort due to constraint violation (UNIQUE constraint failed: Videos.VideoID)
		// temp fix
		try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO Videos (VideoID) VALUES (?)"))
		{
			for (String videoID : videoIDs)
			{
				statement.setString(1, videoID);
				statement.executeUpdate();
			}
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * attempts to link downloaded data to its videoID in the database
	 * @param videoData
	 */
	protected synchronized static void addVideo(NETP_VideoData videoData)
	{
		// check that Videos has this videoID
		try (PreparedStatement statement = connection.prepareStatement("SELECT ID FROM Videos WHERE VideoID=?"))
		{
			statement.setString(1, videoData.getVideoID());
			try (ResultSet resultSet = statement.executeQuery())
			{
				if (resultSet.next() == false) // not existing
					return;
			}
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
		// add VideoData
		int videoDataID = -1;
		try (PreparedStatement statement = connection.prepareStatement(NETP_VideoData.SQL_STATEMENT, Statement.RETURN_GENERATED_KEYS))
		{
			videoData.prepareStatement(statement);
			statement.execute();
			try (ResultSet resultSet = statement.getGeneratedKeys())
			{
				videoDataID = resultSet.getInt(1);
			}
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
		// modify Videos to reference VideoData
		try (PreparedStatement statement = connection.prepareStatement("UPDATE Videos SET VideoDataID=? WHERE VideoID=?"))
		{
			statement.setInt(1, videoDataID);
			statement.setString(2, videoData.getVideoID());
			statement.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * removes a videoID from the database
	 * note: does not remove LINKED videoData
	 * @param videoID
	 */
	protected static void remove(String videoID)
	{
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM Videos WHERE VideoID=?"))
		{
			statement.setString(1, videoID);
			statement.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}
}
