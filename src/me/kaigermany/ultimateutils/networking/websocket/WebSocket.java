package me.kaigermany.ultimateutils.networking.websocket;

import java.io.IOException;
import java.net.Socket;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

import me.kaigermany.ultimateutils.networking.util.SocketFactory;

public class WebSocket extends WebSocketBasic {

	private static final int CHALLENGE_KEY_SIZE = 16;
	private final Socket socket;

	/**
	 * Opens a new WebSocket to the given URL
	 * 
	 * @param url
	 *            URL of the WebSocket Server
	 * @param callback
	 *            Event callbacks
	 * @return Instance of the WebSocket
	 * @throws IOException
	 *             if something stupid happens
	 */
	public static WebSocket open(String url, WebSocketEvent callback) throws IOException {
		return open(url, callback, url.startsWith("wss://"), null);
	}

	/**
	 * Opens a new WebSocket to the given URL
	 * 
	 * @param url
	 *            URL of the WebSocket Server
	 * @param callback
	 *            Event callbacks
	 * @param additionalHeaders
	 *            Headers that should be added
	 * @return instance of the WebSocket
	 * @throws IOException
	 *             if something stupid happens
	 */
	public static WebSocket open(String url, WebSocketEvent callback, Map<String, String> additionalHeaders) throws IOException {
		return open(url, callback, url.startsWith("wss://"), additionalHeaders);
	}

	/**
	 * Opens a new WebSocket to the given URL
	 * 
	 * @param url
	 *            URL of the WebSocket Server
	 * @param callback
	 *            Event callbacks
	 * @param useSSL
	 *            should SSL be used for the WebSocket Connection
	 * @param additionalHeaders
	 *            Headers that should be added
	 * @return instance of the WebSocket
	 * @throws IOException
	 *             if something stupid happens
	 */
	public static WebSocket open(String url, WebSocketEvent callback, boolean useSSL, Map<String, String> additionalHeaders) throws IOException {
		return open(url, callback, useSSL, false, additionalHeaders);
	}
	
	/**
	 * 
	 * @param url
	 *            URL of the WebSocket Server
	 * @param callback
	 *            Event callbacks
	 * @param useSSL
	 *            should SSL be used for the WebSocket Connection
	 * @param disableCertificateCheck
	 *            if useSSL && disableCertificateCheck then the standard
	 *            certificate check will be disabled. USE AT OWN RISK since it
	 *            allows also undetected men-in-the-middle attacks
	 *            theoretically.
	 * @param additionalHeaders
	 *            Headers that should be added
	 * @return instance of the WebSocket
	 * @throws IOException
	 *             if something stupid happens
	 */
	public static WebSocket open(String url, WebSocketEvent callback, boolean useSSL, boolean disableCertificateCheck, Map<String, String> additionalHeaders) throws IOException {
		if (url.startsWith("wss://"))
			url = url.substring(6);
		else if (url.startsWith("ws://"))
			url = url.substring(5);
		int a = url.indexOf('/');
		String serverName;
		String targetPath;
		if (a == -1) {
			serverName = url;
			targetPath = "/";
		} else {
			serverName = url.substring(0, a);
			targetPath = url.substring(a);
		}

		String ip;
		int port;
		a = serverName.indexOf(':');
		if (a == -1) {
			ip = serverName;
			port = useSSL ? 443 : 80;
		} else {
			ip = serverName.substring(0, a);
			port = Integer.parseInt(serverName.substring(a + 1));
		}

		Socket socket = SocketFactory.openConnection(ip, port, useSSL, disableCertificateCheck);
				//useSSL ? getUncheckedSSLSocketFactory().createSocket(ip, port) : new Socket(ip, port);
		return new WebSocket(socket, serverName, targetPath, callback, additionalHeaders);
	}

	/**
	 * Opens a new WebSocket to the given Information
	 * 
	 * @param socket
	 * @param ip
	 * @param targetPath
	 * @param callback
	 *            Event callbacks
	 * @param additionalHeaders
	 *            Headers that should be added
	 * @throws IOException
	 *             if something stupid happens
	 */
	public WebSocket(Socket socket, String ip, String targetPath, WebSocketEvent callback, Map<String, String> additionalHeaders) throws IOException {
		super(socket, callback, false);

		this.socket = socket;
		if (!handleHttpProtocolSwitch(ip, targetPath, additionalHeaders))
			throw new IOException("Can't establish websocket connection");
		
		initThread();
	}

	/**
	 * Switches the Protocol to WebSocket
	 * 
	 * @param ip
	 *            Address of the Server
	 * @param targetPath
	 *            HTTP Path
	 * @param additionalHeaders
	 *            additional Header information
	 * @return true if switch was successful
	 * @throws IOException
	 *             if something stupid happens
	 */
	protected boolean handleHttpProtocolSwitch(String ip, String targetPath, Map<String, String> additionalHeaders) throws IOException {
		String challengeKey = generateRandomChallenge();
		StringBuilder builder = new StringBuilder(2048).append("GET ").append(targetPath).append(" HTTP/1.1\r\n")
				.append("Accept: */*\r\n")
				.append("Connection: keep-alive, Upgrade\r\n")
				.append("DNT: 1\r\n")
				.append("Host: ").append(ip).append("\r\n")
				.append("Pragma: no-cache\r\n")
				.append("Sec-Fetch-Dest: empty\r\n")//websocket
				.append("Sec-Fetch-Mode: websocket\r\n")
				.append("Sec-Fetch-Site: same-site\r\n")
				.append("Sec-WebSocket-Key: ").append(challengeKey).append("\r\n")
				.append("Sec-WebSocket-Version: 13\r\n")
				.append("Upgrade: websocket\r\n")
				.append("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0\r\n\r\n");

		if (additionalHeaders != null) {
			for (Map.Entry<String, String> e : additionalHeaders.entrySet()) {
				builder.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
			}
		}

		dos.write(builder.toString().getBytes());
		dos.flush();

		String l = readLine();
		String[] header = l.split(" ");
		if (!header[1].equals("101")) {
			System.err.println(l);
			do {
				l = readLine();
				System.err.println(l);
			} while (l.length() >= 4);
			return false;
		}

		String keyFilter = "Sec-WebSocket-Accept: ";
		String keyResponse = null;

		// TODO readLine() never return null, l.length() < 4 breaks the loop.
		while ((l = readLine()) != null) {
			if (l.length() < 4) {
				break;
			} else if (l.startsWith(keyFilter)) {
				keyResponse = l.substring(keyFilter.length());
			}
		}

		runKeyCheck(challengeKey, keyResponse);
		return true;
	}

	/**
	 * Generates an random key. It is used to send to the Server, the Server
	 * will do some well known definitions on it and send it back. if the Client
	 * now detect an response that has not the expected modifications then the
	 * connection is maybe a replay or an other illegal state.
	 * 
	 * @return Base64 encoded challenge key for the server.
	 */
	private String generateRandomChallenge() {
		byte[] challenge = new byte[CHALLENGE_KEY_SIZE];
		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < CHALLENGE_KEY_SIZE; i++){
			challenge[i] = (byte) (r.nextInt() & 0xFF);
		}
		return new String(Base64.getEncoder().encode(challenge));
	}

	/**
	 * Compares the received key and checks if the modification is equal to the
	 * expected one.
	 * 
	 * @param challengeKey
	 * @param challengeKeyResponse
	 * @throws IOException
	 */
	private void runKeyCheck(String challengeKey, String challengeKeyResponse) throws IOException {
		String expectedKey = calculateResponseSecret(challengeKey);
		if (!expectedKey.equals(challengeKeyResponse)) throw new IOException("Sec-WebSocket-Accept does not give back an valid key, you are may run into an repeat attac!");
	}
	
	/**
	 * sends a close-message to the Server and exits the connection then.
	 */
	@Override
	public void close() {
		try {
			writeFrame(new byte[0], 0x8);
			super.close();
			socket.close();
		} catch (IOException e) {
			alive = false;
			event.onError(e.getMessage(), e);
		}
	}
}
