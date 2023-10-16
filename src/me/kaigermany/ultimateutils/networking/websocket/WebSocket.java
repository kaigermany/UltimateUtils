package me.kaigermany.ultimateutils.networking.websocket;

import java.io.IOException;
import java.net.Socket;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

public class WebSocket extends WebSocketBasic {

	private static final int CHALLENGE_KEY_SIZE = 16;
	private final Socket socket;

	/**
	 * Opens a new WebSocket to the given URL
	 * @param url URL of the WebSocket Server
	 * @param callback Event callbacks
	 * @return Instance of the WebSocket
	 * @throws IOException if something stupid happens
	 */
	public static WebSocket open(String url, IWebSocketEvent callback) throws IOException {
		return open(url, callback, url.startsWith("wss://"), null);
	}

	/**
	 * Opens a new WebSocket to the given URL
	 * @param url URL of the WebSocket Server
	 * @param callback Event callbacks
	 * @param additionalHeaders Headers that should be added
	 * @return instance of the WebSocket
	 * @throws IOException if something stupid happens
	 */
	public static WebSocket open(String url, IWebSocketEvent callback, Map<String, String> additionalHeaders) throws IOException {
		return open(url, callback, url.startsWith("wss://"), additionalHeaders);
	}

	/**
	 * Opens a new WebSocket to the given URL
	 * @param url URL of the WebSocket Server
	 * @param callback Event callbacks
	 * @param useSSL should SSL be used for the WebSocket Connection
	 * @param additionalHeaders Headers that should be added
	 * @return instance of the WebSocket
	 * @throws IOException if something stupid happens
	 */
	public static WebSocket open(String url, IWebSocketEvent callback, boolean useSSL, Map<String, String> additionalHeaders) throws IOException {
		if(url.startsWith("wss://")) url = url.substring(6);
		else if(url.startsWith("ws://")) url = url.substring(5);
		int a = url.indexOf('/');
		String serverName;
		String targetPath;
		if(a == -1){
			serverName = url;
			targetPath = "/";
		} else {
			serverName = url.substring(0, a);
			targetPath = url.substring(a);
		}

		String ip;
		int port;
		a = serverName.indexOf(':');
		if(a == -1) {
			ip = serverName;
			port = useSSL ? 443 : 80;
		} else {
			ip = serverName.substring(0, a);
			port = Integer.parseInt(serverName.substring(a + 1));
		}

		Socket socket = useSSL ? getUncheckedSSLSocketFactory().createSocket(ip, port) : new Socket(ip, port);
		return new WebSocket(socket, serverName, targetPath, callback, additionalHeaders);
	}

	/**
	 * Opens a new WebSocket to the given Information
	 * @param socket
	 * @param ip
	 * @param targetPath
	 * @param callback Event callbacks
	 * @param additionalHeaders Headers that should be added
	 * @throws IOException if something stupid happens
	 */
	public WebSocket(Socket socket, String ip, String targetPath, IWebSocketEvent callback, Map<String, String> additionalHeaders) throws IOException {
		super(socket, callback);

		this.socket = socket;
		if(!handleHttpProtocolSwitch(ip, targetPath, additionalHeaders)) throw new IOException("Can't establish websocket connection");
		Thread t = new Thread(() -> {
            try {
				event.onOpen(this);
                receivePackets();
            } catch(Exception ex) {
                ex.printStackTrace();
                if(alive) callback.onError(ex.getMessage());
                alive = false;
            }
        });
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Switches the Protocol to WebSocket
	 * @param ip Address of the Server
	 * @param targetPath HTTP Path
	 * @param additionalHeaders additional Header information
	 * @return true if switch was successful
	 * @throws IOException if something stupid happens
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

		if(additionalHeaders != null){
			for(Map.Entry<String, String> e : additionalHeaders.entrySet()){
				builder.append(e.getKey()).append(":").append(e.getValue()).append("\r\n");
			}
		}

		dos.write(builder.toString().getBytes());
		dos.flush();

		String l = readLine();
		String[] header = l.split(" ");
		if(!header[1].equals("101")){
			System.err.println(l);
			do {
				l = readLine();
				System.err.println(l);
			} while (l.length() >= 4);
			return false;
		}

		String keyFilter = "Sec-WebSocket-Accept: ";
		String keyResponse = null;

		while((l = readLine()) != null){
			if(l.length() < 4){
				break;
			} else if (l.startsWith(keyFilter)) {
				keyResponse = l.substring(keyFilter.length());
			}
		}

		runAntiReplayAttackCheck(challengeKey, keyResponse);
		return true;
	}

	/**
	 * TODO COMMENT @KaiGermany
	 * @return
	 */
	private String generateRandomChallenge() {
		byte[] challenge = new byte[CHALLENGE_KEY_SIZE];
		Random r = new Random(System.currentTimeMillis());
		for(int i=0; i<CHALLENGE_KEY_SIZE; i++) challenge[i] = (byte)(r.nextInt() & 0xFF);
		return new String(Base64.getEncoder().encode(challenge));
	}

	/**
	 * TODO COMMENT @KaiGermany
	 * @param challengeKey
	 * @param challengeKeyResponse
	 * @throws IOException
	 */
	private void runAntiReplayAttackCheck(String challengeKey, String challengeKeyResponse) throws IOException {
		String expectedKey = calculateResponseSecret(challengeKey);
		if(!expectedKey.equals(challengeKeyResponse)) throw new IOException("Sec-WebSocket-Accept does not give back an valid key, you are may run into an repeat attac!");
	}

	@Override
	public void close(){
		try{
			writeFrame(new byte[0], 0x8);
			super.close();
			socket.close();
		}catch(IOException t){
			t.printStackTrace();
			alive = false;
			event.onError(t.getMessage());
		}
	}
}
