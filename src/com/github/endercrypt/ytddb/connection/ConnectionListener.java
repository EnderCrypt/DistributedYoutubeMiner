package com.github.endercrypt.ytddb.connection;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public abstract class ConnectionListener implements Runnable, Closeable
{
	public final Socket socket;
	public final String IP;
	public final ObjectOutputStream objectOutputStream;
	public final ObjectInputStream objectInputStream;

	private Thread socketListenerThread;
	private Map<Class<?>, ReceivedListener<?>> receiveListeners = new HashMap<>();

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

	public void send(Serializable serializable)
	{
		try
		{
			objectOutputStream.writeObject(serializable);
			objectOutputStream.flush();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException
	{
		socketListenerThread.interrupt();
		socket.close();
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
					close();
					throw new RuntimeException("Missing ReceiverListener for " + receivedClass.getName());
				}
				else
				{
					receivedListener.onReceive(convertObject(receivedClass, receivedObject));
				}
			}
		}
		catch (Exception e)
		{
			System.out.println(IP + " Connection Broke: [" + e.getClass().getSimpleName() + "] " + e.getMessage());
			try
			{
				close();
			}
			catch (IOException ce)
			{
				System.err.println("Error occured while trying to close a connection: " + ce.getMessage());
			}
		}
	}

	private <T extends Serializable> T convertObject(Class<?> receivedClass, Object receivedObject)
	{
		return (T) receivedObject;
	}
}
