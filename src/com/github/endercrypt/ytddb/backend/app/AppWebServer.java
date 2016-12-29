package com.github.endercrypt.ytddb.backend.app;

import fi.iki.elonen.NanoHTTPD;

public class AppWebServer extends NanoHTTPD
{
	private BackendApp backendApp;

	public AppWebServer(BackendApp backendApp)
	{
		super(8080);
		this.backendApp = backendApp;
	}

	@Override
	public Response serve(IHTTPSession session)
	{
		String loginCookie = session.getCookies().read("id");
		String uri = session.getUri();

		switch (uri)
		{
		case "":

			break;
		}

		return newFixedLengthResponse("uri: " + uri);
	}

}
