package me.kaigermany.ultimateutils.networking.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;

public class WebSocketServer extends WebSocketBasic {

	/**
	 * Handle given streams as WebSocket endpoint
	 * @param is InputStream
	 * @param os OutputStream
	 * @param event Event callbacks
	 * @throws IOException if communications fail in some way.
	 */
	public WebSocketServer(InputStream is, OutputStream os, WebSocketEvent event) throws IOException {
		super(is, os, event, true);
		init();
		initThread();
	}

	/**
	 * Handle given socket as WebSocket endpoint
	 * @param socket accepted Socket of the webserver
	 * @param event Event callbacks
	 * @throws IOException if communications fail in some way.
	 */
	public WebSocketServer(Socket socket, WebSocketEvent event) throws IOException {
		super(socket, event, true);
		init();
		initThread();
	}

	/**
	 * Handle given streams as WebSocket endpoint
	 * @param is InputStream
	 * @param os OutputStream
	 * @param event Event callbacks
	 * @throws IOException if communications fail in some way.
	 */
	public WebSocketServer(InputStream is, OutputStream os, WebSocketEvent event, Map<String, String> httpHeaders) throws IOException {
		super(is, os, event, true);
		init(httpHeaders);
		initThread();
	}

	/**
	 * Handle given socket as WebSocket endpoint
	 * @param socket accepted Socket of the webserver
	 * @param event Event callbacks
	 * @throws IOException if communications fail in some way.
	 */
	public WebSocketServer(Socket socket, WebSocketEvent event, Map<String, String> httpHeaders) throws IOException {
		super(socket, event, true);
		init(httpHeaders);
		initThread();
	}

	/**
	 * Initiates the WebSocket Upgrade
	 * @throws IOException if read fails.
	 */
	protected void init() throws IOException {
		String r;
		String keyFilter = "Sec-WebSocket-Key: ";
		String key = null;

		do {
			r = readLine();
			if (r.startsWith(keyFilter)) {
				key = r.substring(keyFilter.length());
			}
		} while (r.length() >= 4);

		sendResponseKey(key);
	}

	/**
	 * Initiates the WebSocket Upgrade
	 * @throws IOException if write fails.
	 */
	protected void init(Map<String, String> header) throws IOException {
		String key = header.get("Sec-WebSocket-Key");

		if (key == null) {
			throw new IllegalStateException("Websocket header has no Sec-WebSocket-Key Header");
		}

		sendResponseKey(key);
	}
	
	/*
	 * Converts the given key and submit the generated response.
	 * @throws IOException if write fails.
	 */
	protected void sendResponseKey(String key) throws IOException {
		key = calculateResponseSecret(key);
		dos.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: " + key + "\r\n\r\n").getBytes());
		dos.flush();
	}
}
