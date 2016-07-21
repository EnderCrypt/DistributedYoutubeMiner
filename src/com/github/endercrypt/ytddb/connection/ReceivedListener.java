package com.github.endercrypt.ytddb.connection;

import java.io.Serializable;

public interface ReceivedListener<T extends Serializable>
{
	void onReceive(T object);
}
