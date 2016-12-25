package com.github.endercrypt.ytddb;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.github.endercrypt.ytddb.backend.Backend;
import com.github.endercrypt.ytddb.backend.DataCenter;
import com.github.endercrypt.ytddb.connection.Client;

public class Main
{
	private static final int PORT = 36963;
	private static String DB_FILE = "YoutubeVideoData.db";

	public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException, InterruptedException
	{
		String ip = null;
		if (args.length == 1)
		{
			ip = args[0];
		}
		InetAddress address = InetAddress.getLocalHost();
		if (ip == null)
		{
			System.out.println("Running server app..");
			try
			{
				Backend.init(PORT);
			}
			catch (BindException e)
			{
				Thread.sleep(1);
				System.err.println("ERROR, Port " + PORT + " already in use");
				System.exit(0);
			}
			System.out.println("Initializing SQL...");
			Class.forName("org.sqlite.JDBC");
			DataCenter.init(DriverManager.getConnection("jdbc:sqlite:" + DB_FILE));
			System.out.println("Running as server");
			Backend.run();
		}
		else
		{
			try
			{
				address = Inet4Address.getByName(ip);
			}
			catch (UnknownHostException e)
			{
				System.err.println("Unknown host");
				System.exit(1);
			}
			System.out.print("Connecting to client... ");
			connectTo(address);
		}
	}

	@SuppressWarnings("resource")
	private static void connectTo(InetAddress address) throws IOException
	{
		Socket socket = null;
		try
		{
			socket = new Socket(address, PORT);
		}
		catch (ConnectException e)
		{
			System.err.println("Failed to connect: " + e.getMessage());
			return;
		}
		System.out.println("Connected!");
		Client connection = new Client(socket);
		connection.startListening();
	}
}
