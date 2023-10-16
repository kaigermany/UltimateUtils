package me.kaigermany.ultimateutils.networking.websocket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Random;

public class WebSocketBasic {

    protected volatile boolean alive = true;
    protected final DataInputStream dis;
    protected final DataOutputStream dos;
    protected final IWebSocketEvent event;
    private boolean isServer = false;

    public WebSocketBasic(Socket socket, IWebSocketEvent event) throws IOException {
        this.event = event;
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
    }

    public WebSocketBasic(InputStream is, OutputStream os, IWebSocketEvent event) {
        dis = is instanceof DataInputStream ? (DataInputStream)is :  new DataInputStream(is);
        dos = os instanceof DataOutputStream ? (DataOutputStream)os :  new DataOutputStream(os);
        this.event = event;
    }

    /**
     * Used to set the maskMode
     * @param server enable or disable
     */
    public void setServer(boolean server) {
        isServer = server;
    }

    /**
     * Sends data as String to the endpoint
     * @param data Text content
     */
    public void send(String data) {
        try {
            writeFrame(data.getBytes(StandardCharsets.UTF_8), 0x01);
        } catch (IOException ex) {
            event.onError("Failed to write frame: " + ex.getMessage());
        }
    }

    /**
     * Sends data as raw Bytes to the endpoint
     * @param data Bytes to send
     */
    public void send(byte[] data) {
        try {
            writeFrame(data, 0x02);
        } catch (IOException ex) {
            event.onError("Failed to write frame: " + ex.getMessage());
        }
    }

    /**
     * Closes the connection to the endpoint
     */
    public void close() {
        alive = false;
        event.onClose();
    }

    /**
     * Constantly reads from the InputStream and processes the data
     * @throws IOException if something stupid happens
     */
    protected void receivePackets() throws IOException {
        ByteArrayOutputStream multiPacketBuffer = null;
        while (alive) {
            int chr = dis.read();
            if(chr == -1) return;
            boolean fin = (chr & 128) != 0;
            int opCode = chr & 0x0F;
            byte[] data = readNextFrame(dis);
            if (opCode <= 2) {
                if (!fin) {
                    if (multiPacketBuffer == null) multiPacketBuffer = new ByteArrayOutputStream();
                    multiPacketBuffer.write(data);
                } else {
                    if (multiPacketBuffer == null) {
                        if (opCode == 1)
                            event.onMessage(new String(data, StandardCharsets.UTF_8), this);
                        else if (opCode == 2)
                            event.onBinary(data, this);
                        continue;
                    }
                    multiPacketBuffer.write(data);

                    if (opCode == 1)
                        event.onMessage(new String(multiPacketBuffer.toByteArray(), StandardCharsets.UTF_8), this);
                    else if (opCode == 2)
                        event.onBinary(multiPacketBuffer.toByteArray(), this);

                    multiPacketBuffer = null;
                }
            } else if (opCode == 9) {
                writeFrame(data, 0x0A);
            } else if (opCode == 8) {
                if (data.length > 0) event.onError(new String(data));
                event.onClose();
                return;
            }
        }
    }

    /**
     * Reads a line (until the next '\n') from the current InputStream
     * @return Read line from the InputStream
     * @throws IOException if something stupid happens
     */
    protected String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int chr;
        while((chr = dis.read()) != -1 && chr != '\n'){
            sb.append((char)chr);
        }
        String s = sb.toString();
        if(s.charAt(s.length() - 1) == '\r') return s.substring(0, s.length() - 1);
        return s;
    }

    /**
     * Writes an WebSocket frame
     * <a href="https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_WebSocket_servers">...</a>
     * <a href="https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_a_WebSocket_server_in_Java">...</a>
     *
     * @param data Raw Data to send
     * @param opCode opCode
     * @throws IOException
     */
    public void writeFrame(byte[] data, int opCode) throws IOException {
        // masking as Server is not allowed :(
        // https://datatracker.ietf.org/doc/html/rfc6455#section-5.1
        // -> "A server MUST NOT mask any frames that it sends to the client."

        ByteArrayOutputStream writeBuf = new ByteArrayOutputStream(data.length + 10);//the overhead can't be lager then 10 bytes.
        DataOutputStream d = new DataOutputStream(writeBuf);

        writeBuf.write(128 | (opCode & 0x0F));//opCode & FIN flag

        {
            int lenByte = !isServer ? 0x80 : 0;
            if(data.length < 126){
                lenByte |= data.length;
            } else if(data.length <= 0xFFFF){
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
			/* this is maybe fast, but on large data sets i can do better.
			for (int i = 0; i < data.length; i++) {
				d.write(data[i] ^ (mask >> ((3 - (i & 3)) << 3)) & 0xFF);
			}
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
     * Creates a new SSLSocketFactory that ignores all certificate validations
     * based on:
     * <a href="https://stackoverflow.com/questions/12060250/ignore-ssl-certificate-errors-with-java">...</a>
     *
     * @return SSLSocketFactory
     * @throws IOException if something stupid happens
     */
    protected static SSLSocketFactory getUncheckedSSLSocketFactory() throws IOException {
        TrustManager[] certs = new TrustManager[] { new X509TrustManager() {
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
        }};

        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("SSL");
            ctx.init(null, certs, new SecureRandom());
        } catch (java.security.GeneralSecurityException ex) {
            throw new IOException(ex.getMessage());
        }
        return ctx.getSocketFactory();
    }

    public static String calculateResponseSecret(byte[] challengeKey){
        return calculateResponseSecret(new String(challengeKey));
    }

    /**
     * TODO COMMENT @KaiGermany
     * @param challengeKey
     * @return
     */
    public static String calculateResponseSecret(String challengeKey){
        String key = challengeKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return new String(Base64.getEncoder().encode(md.digest(key.getBytes())));
    }

    private static byte[] maskToBytes(int mask){
        byte[] out = new byte[4];
        for (int i = 0; i < 4; i++) {
            out[i] = (byte)((mask >> ((3 - i) << 3)) & 0xFF);
        }
        return out;
    }

    private static void applyMask(DataOutputStream dos, byte[] data, byte[] maskBytes) throws IOException {//copying version
        for (int i = 0; i < data.length; i++) {
            dos.write(data[i] ^ maskBytes[i & 3]);
        }
    }

    private static void applyMask(byte[] data, byte[] maskBytes) {//modifying version
        for (int i = 0; i < data.length; i++) {
            data[i] ^= maskBytes[i & 3];
        }
    }

    public static byte[] readNextFrame(DataInputStream dis) throws IOException {
        int len = dis.read();
        int mask = (len & 0x80);
        len &= 0x7F;
        if (len == 126) {
            len = dis.readUnsignedShort();
        } else if (len == 127) {
            len = (int)dis.readLong();
        }
        if (mask != 0) {
            mask = dis.readInt();
        }
        byte[] data = new byte[len];
        dis.readFully(data);

        if (mask != 0) applyMask(data, maskToBytes(mask));	//in the absolutely rare chance that responded mask == 0 the mask processor can be skipped, too, without any unexpected error.

        return data;
    }
}
