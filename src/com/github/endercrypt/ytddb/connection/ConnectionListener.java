package com.github.endercrypt.ytddb.connection;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.github.endercrypt.ytddb.backend.BackendConnection;
import com.github.endercrypt.ytddb.net.NETP_ConnectionException;

public abstract class ConnectionListener implements Runnable, Closeable
{
	public final Socket socket;
	public final String IP;
	public final ObjectOutputStream objectOutputStream;
	public final ObjectInputStream objectInputStream;

	private Thread socketListenerThread;
	private Map<Class<?>, ReceivedListener<?>> receiveListeners = new HashMap<>();
	private boolean connectionOnline = true;

	public ConnectionListener(Socket socket) throws IOException
	{
		this.socket = socket;
		IP = socket.getInetAddress().toString();
		objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		objectInputStream = new ObjectInputStream(socket.getInputStream());
	}

	protected void setObjectListener(Class<?> clazz, ReceivedListener<?> receivedListener)
	{
		receiveListeners.put(clazz, receivedListener);
	}

	public Thread startListening()
	{
		socketListenerThread = new Thread(this);
		socketListenerThread.start();
		return socketListenerThread;
	}

	public void send(Serializable serializable) throws IOException
	{
		objectOutputStream.writeObject(serializable);
		objectOutputStream.flush();
	}

	protected void panic(Exception e)
	{
		if (connectionOnline)
		{
			System.out.println(IP + " Connection Broke: [" + e.getClass().getSimpleName() + "] " + e.getMessage());
			try
			{
				if (getClass().equals(BackendConnection.class))
				{
					send(new NETP_ConnectionException(e));
				}
				if (getClass().equals(Client.class))
				{
					System.err.println("An exception occured in this client, please report this to Magnus/EnderCrypt");
					e.printStackTrace();
				}
				close();
			}
			catch (IOException ce)
			{
				// failed to close properly, ignored
			}
		}
	}

	@Override
	public void close() throws IOException
	{
		if (connectionOnline)
		{
			System.out.println("Disconnected: " + IP);
			connectionOnline = false;
			socketListenerThread.interrupt();
			socket.close();
		}
	}

	@Override
	public void run()
	{
		try
		{
			while (!Thread.interrupted())
			{
				Object receivedObject = objectInputStream.readObject();
				Class<?> receivedClass = receivedObject.getClass();
				ReceivedListener<?> receivedListener = receiveListeners.get(receivedClass);
				if (receivedListener == null)
				{
					//close();
					//throw new RuntimeException("Missing ReceiverListener for " + receivedClass.getName());
				}
				else
				{
					receivedListener.onReceive(convertObject(receivedClass, receivedObject));
				}
			}
		}
		catch (Exception e)
		{
			panic(e);
		}
	}

	@SuppressWarnings({ "unused", "unchecked" })
	private <T extends Serializable> T convertObject(Class<?> receivedClass, Object receivedObject)
	{
		return (T) receivedObject;
	}
}
