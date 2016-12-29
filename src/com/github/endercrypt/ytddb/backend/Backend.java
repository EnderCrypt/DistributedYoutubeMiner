package com.github.endercrypt.ytddb.backend;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import com.github.endercrypt.ytddb.backend.app.BackendApp;

public class Backend
{
	private static ServerSocket serverSocket;
	public static final Set<BackendConnection> connections = new HashSet<>();

	public static BackendApp app;

	public static void init(int port) throws IOException
	{
		serverSocket = new ServerSocket(port);
		app = new BackendApp();
		new Thread(app).start();
	}

	public static void run() throws IOException
	{
		while (true)
		{
			@SuppressWarnings("resource") Socket socket = serverSocket.accept();
			@SuppressWarnings("resource") BackendConnection connection = new BackendConnection(socket);
			System.out.println("Received connection from " + connection.IP);
			connection.startListening();
			connections.add(connection);
		}
	}
}
