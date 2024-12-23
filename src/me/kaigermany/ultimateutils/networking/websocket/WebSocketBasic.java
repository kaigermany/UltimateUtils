package me.kaigermany.ultimateutils.networking.websocket;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

public abstract class WebSocketBasic {
	protected static final int OPCODE_CONTINUATION_FRAME = 0x00;
	protected static final int OPCODE_TEXT_FRAME = 0x01;
	protected static final int OPCODE_BINARY_FRAME = 0x02;
	
	protected static final int OPCODE_CONNECTION_CLOSE = 0x08;
	protected static final int OPCODE_PING = 0x09;
	protected static final int OPCODE_PONG = 0x0A;
	
	
	protected volatile boolean alive = true;
	protected final DataInputStream dis;
	protected final DataOutputStream dos;
	protected final WebSocketEvent event;
	private final boolean isServer;

	public WebSocketBasic(Socket socket, WebSocketEvent event, boolean isServer) throws IOException {
		dis = new DataInputStream(socket.getInputStream());
		dos = new DataOutputStream(socket.getOutputStream());
		this.event = event;
		this.isServer = isServer;
	}

	public WebSocketBasic(InputStream is, OutputStream os, WebSocketEvent event, boolean isServer) throws IOException {
		dis = is instanceof DataInputStream ? (DataInputStream) is : new DataInputStream(is);
		dos = os instanceof DataOutputStream ? (DataOutputStream) os : new DataOutputStream(os);
		this.event = event;
		this.isServer = isServer;
	}
	
	/**
	 * Starts the internal listener Thread. Must be started by child class. this
	 * allows the WebSocket-Client to handle HTTP protocol switch before
	 * launching the listener
	 * 
	 * @throws IOException
	 */
	protected void initThread() throws IOException {
		Thread t = new Thread(() -> {
			try {
				event.onOpen(this);
				receivePackets();
			} catch (Exception ex) {
				if(alive) {
					event.onError(this, ex.getMessage(), ex);
				}
				alive = false;
			}
		});
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Sends data as String to the endpoint
	 * 
	 * @param data
	 *            Text content
	 */
	public void send(String data) {
		try {
			writeFrame(data.getBytes(StandardCharsets.UTF_8), OPCODE_TEXT_FRAME);
		} catch (IOException ex) {
			event.onError(this, "Failed to write frame: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Sends data as raw Bytes to the endpoint
	 * 
	 * @param data
	 *            Bytes to send
	 */
	public void send(byte[] data) {
		try {
			writeFrame(data, OPCODE_BINARY_FRAME);
		} catch (IOException ex) {
			event.onError(this, "Failed to write frame: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Closes the connection to the endpoint
	 */
	public void close() {
		alive = false;
		event.onClose(this);
	}

	/**
	 * Constantly reads from the InputStream and processes the data
	 * 
	 * @throws IOException
	 *             if something stupid happens
	 */
	protected void receivePackets() throws IOException {
		//The buffer is used to buffer packets that will be transmitted
		//over several frames. if a incomplete-frame was received,
		//it will be appended to the buffer until an end-frame is received.
		ByteArrayOutputStream multiPacketBuffer = null;
		while (alive) {
			int chr = dis.read();
			
			if (chr == -1)
				return;
			
			boolean fin = (chr & 128) != 0;
			int opCode = chr & 0x0F;
			byte[] data = readNextFrame(dis);
			if (opCode <= 2) {
				if (!fin) {
					
					if (multiPacketBuffer == null)
						multiPacketBuffer = new ByteArrayOutputStream();
					
					multiPacketBuffer.write(data);
				} else {
					if (multiPacketBuffer == null) {
						
						if (opCode == OPCODE_TEXT_FRAME)
							event.onMessage(new String(data, StandardCharsets.UTF_8), this);
						else if (opCode == OPCODE_BINARY_FRAME)
							event.onBinary(data, this);
						
						continue;
					}
					multiPacketBuffer.write(data);

					if (opCode == OPCODE_TEXT_FRAME)
						event.onMessage(new String(multiPacketBuffer.toByteArray(), StandardCharsets.UTF_8), this);
					else if (opCode == OPCODE_BINARY_FRAME)
						event.onBinary(multiPacketBuffer.toByteArray(), this);

					multiPacketBuffer = null;
				}
			} else if (opCode == OPCODE_PING) {
				writeFrame(data, OPCODE_PONG);
			} else if (opCode == OPCODE_CONNECTION_CLOSE) {
				if (data.length > 0){
					String errorText = new String(data, StandardCharsets.UTF_8);
					event.onError(this, "Connection close message: " + errorText, new SocketException(errorText));
				}
				event.onClose(this);
				return;
			}
		}
	}

	/**
	 * Reads a line (until the next '\n') from the current InputStream
	 * 
	 * @return Read line from the InputStream
	 * @throws IOException
	 *             if something stupid happens
	 */
	protected String readLine() throws IOException {
		StringBuilder sb = new StringBuilder();
		int chr;
		while ((chr = dis.read()) != -1 && chr != '\n') {
			sb.append((char) chr);
		}
		String s = sb.toString();
		if (s.charAt(s.length() - 1) == '\r') return s.substring(0, s.length() - 1);
		return s;
	}

	/**
	 * Writes an WebSocket frame. See: <a href=
	 * "https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_WebSocket_servers">
	 * Writing_WebSocket_servers</a> <a href=
	 * "https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_a_WebSocket_server_in_Java">
	 * Writing_a_WebSocket_server_in_Java</a>
	 *
	 * NOTE: masking as Server is not allowed :( <br/>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.1">https
	 * ://datatracker.ietf.org/doc/html/rfc6455#section-5.1</a><br/>
	 * -> "A server MUST NOT mask any frames that it sends to the client."
	 * 
	 * @param data
	 *            Raw Data to send
	 * @param opCode
	 *            opCode
	 * @throws IOException
	 */
	protected void writeFrame(byte[] data, int opCode) throws IOException {
		// the overhead can't be lager then 10 bytes.
		ByteArrayOutputStream writeBuf = new ByteArrayOutputStream(data.length + 10);
		DataOutputStream d = new DataOutputStream(writeBuf);

		writeBuf.write(128 | (opCode & 0x0F));// opCode & FIN flag

		{
			int lenByte = !isServer ? 0x80 : 0;
			if (data.length < 126) {
				lenByte |= data.length;
			} else if (data.length <= 0xFFFF) {
				lenByte |= 126;
			} else {
				lenByte |= 127;
			}
			writeBuf.write(lenByte);

			if (lenByte == 126) {
				d.writeShort(data.length);
			} else if (lenByte == 127) {
				d.writeLong(data.length);
			}
		}

		if (!isServer) {
			int mask = new Random(System.currentTimeMillis()).nextInt();
			d.writeInt(mask);
			/*
			 * this is maybe fast, but on large data sets i can do better. for
			 * (int i = 0; i < data.length; i++) { d.write(data[i] ^ (mask >>
			 * ((3 - (i & 3)) << 3)) & 0xFF); }
			 */
			applyMask(d, data, maskToBytes(mask));
			writeBuf.writeTo(dos);
		} else {
			writeBuf.writeTo(dos);
			dos.write(data);
		}
		dos.flush();
	}

	/**
	 * Generate response secret according to
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455">the official
	 * specifications</a>.<br/>
	 * 
	 * @param challengeKey the given challenge key
	 * @return the new challenge key
	 */
	protected static String calculateResponseSecret(byte[] challengeKey) {
		return calculateResponseSecret(new String(challengeKey));
	}

	/**
	 * Generate response secret according to
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455">the official
	 * specifications</a>.<br/>
	 * 
	 * @param challengeKey the given challenge key
	 * @return the new challenge key
	 */
	protected static String calculateResponseSecret(String challengeKey) {
		String key = challengeKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return new String(Base64.getEncoder().encode(md.digest(key.getBytes())));
	}

	/**
	 * Maps a given int to a new byte[4].
	 * 
	 * @param mask
	 * @return
	 */
	private static byte[] maskToBytes(int mask) {
		byte[] out = new byte[4];
		for (int i = 0; i < 4; i++) {
			out[i] = (byte) ((mask >> ((3 - i) << 3)) & 0xFF);
		}
		return out;
	}

	/**
	 * Ultrafast implementation to mask given byte[] and write the output
	 * directly onto DataOutputStream.
	 * 
	 * @param dos
	 * @param data
	 * @param maskBytes
	 * @throws IOException
	 */
	private static void applyMask(DataOutputStream dos, byte[] data, byte[] maskBytes) throws IOException {// copying
																											// version
		for (int i = 0; i < data.length; i++) {
			dos.write(data[i] ^ maskBytes[i & 3]);
		}
	}

	/**
	 * Ultrafast implementation to mask or unmask a given byte[].
	 * 
	 * @param data
	 * @param maskBytes
	 */
	private static void applyMask(byte[] data, byte[] maskBytes) {// modifying
																	// version
		for (int i = 0; i < data.length; i++) {
			data[i] ^= maskBytes[i & 3];
		}
	}

	/**
	 * Reads a full frame from DataInputStream and unmask it if needed.
	 * Note that extreme long transmissions are not supported!
	 * @param dis
	 * @return
	 * @throws IOException
	 */
	public static byte[] readNextFrame(DataInputStream dis) throws IOException {
		int len = dis.read();
		int mask = (len & 0x80);
		len &= 0x7F;
		if (len == 126) {
			len = dis.readUnsignedShort();
		} else if (len == 127) {
			long len_long = dis.readLong();
			len = (int)(len_long & 0x7FFFFFFFL);
			if(len_long != (long)len) {
				throw new IOException("packet is too long for this implementation! Maximum supported is 2GB of data caused by the limits of 'new byte[len]'.");
			}
		}
		if (mask != 0) {
			mask = dis.readInt();
		}
		byte[] data = new byte[len];
		dis.readFully(data);

		if (mask != 0)
			applyMask(data, maskToBytes(mask));
		// in the absolutely rare chance  that responded mask == 0 the
		// mask processor can be  skipped, too, without any  unexpected error.
		return data;
	}
}
