package me.kaigermany.ultimateutils.networking.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class WebSocketServer extends WebSocketBasic {

	/**
	 * Handle given streams as WebSocket endpoint
	 * @param is InputStream
	 * @param os OutputStream
	 * @param event Event callbacks
	 * @throws IOException if something stupid happens
	 */
	public WebSocketServer(InputStream is, OutputStream os, IWebSocketEvent event) throws IOException {
		super(is, os, event);
		setServer(true);
		initConnection();
		new Thread(this::run).start();
	}

	/**
	 * Handle given socket as WebSocket endpoint
	 * @param socket accepted Socket of the webserver
	 * @param event Event callbacks
	 * @throws IOException if something stupid happens
	 */
	public WebSocketServer(Socket socket, IWebSocketEvent event) throws IOException {
		super(socket, event);
		setServer(true);
		initConnection();
		new Thread(this::run).start();
	}

	/**
	 * Initiates the WebSocket Upgrade
	 * @throws IOException if something stupid happens
	 */
	private void initConnection() throws IOException {
		String r;
		String keyFilter = "Sec-WebSocket-Key: ";
		String key = null;

		do {
			r = readLine();
			if (r.startsWith(keyFilter)) {
				key = r.substring(keyFilter.length());
			}
		} while (r.length() >= 4);

		key = calculateResponseSecret(key);
		
		dos.write(("HTTP/1.1 101 Switching Protocols\n" + "Upgrade: websocket\r\n" + "Connection: Upgrade\r\n"
				+ "Sec-WebSocket-Accept: " + key + "\r\n" +
				"\r\n").getBytes());
		dos.flush();
	}

	public void run() {
		try {
			event.onOpen(this);
			receivePackets();
		} catch (Exception e) {
			e.printStackTrace();
			alive = false;
			event.onError("");
		}
	}
}
