package me.kaigermany.ultimateutils.networking.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map.Entry;

//TODO test
public class HTTPClient {
	private static final HashMap<String, String> defaultHeader;
	private static final byte[] DUMMY_BUFFER = new byte[4096];
	static{
		defaultHeader = new HashMap<String, String>(11);
		defaultHeader.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:75.0) Gecko/20100101 Firefox/75.0");
		defaultHeader.put("Connection", "keep-alive");
		defaultHeader.put("DNT", "1");
	}
	private long maxAge;
	private volatile boolean isInUse = false;
	private volatile boolean disabled = false;
	private Socket socket;
	private InputStream is;
	private OutputStream os;
	private String host;
	public HTTPClient(String server, int port, boolean ssl, boolean disableCertificateCheck) throws UnknownHostException, IOException {
		socket = SocketFactory.openConnection(server, port, ssl, disableCertificateCheck);
		host = server;
		is = socket.getInputStream();
		os = socket.getOutputStream();
		//socket.setSoTimeout(10_000);
		resetAge();
	}

	public HTTPResult request(String page, String requestMethod, HashMap<String, String> headerFields, byte[] postData, HTTPResultEvent event) throws IOException {
		resetAge();
		/*synchronized (this) {
			isInUse = true;
		}*/
		if(page == null || page.length() == 0 || page.charAt(0) != '/') page = "/" + (page == null ? "" : page);
		if(requestMethod == null) requestMethod = "GET";
		if(headerFields == null) headerFields = new HashMap<String, String>();
		
		for(Entry<String, String> e : defaultHeader.entrySet()){
			if(headerFields.containsKey(e.getKey())) continue;
			headerFields.put(e.getKey(), e.getValue());
		}
		headerFields.putIfAbsent("Host", host);
		
		if(postData != null){
			headerFields.put("Content-Length", String.valueOf(postData.length));
		}
		
		StringBuilder sb = new StringBuilder(requestMethod).append(' ').append(page).append(" HTTP/1.1\r\n");
		for(Entry<String, String> e : headerFields.entrySet()){
			sb.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
		}
		
		
		os.write(sb.append("\r\n").toString().getBytes());
		if(postData != null){
			os.write(postData);
		}
		os.flush();
		
		String len = null;
		int responseCode;
		HashMap<String, String> resultHeader = new HashMap<String, String>();
		boolean isChunkedEncoding = false;
		{
			String line = readLine(is);
			String[] tmp = line.split(" ");
			if(tmp.length < 2){
				return new HTTPResult(null, null, 0);
			}
			responseCode = Integer.parseInt(tmp[1]);
			//if(!line.endsWith("200 OK")) return null;
			while((line = readLine(is)).length() > 2){
				int p = line.indexOf(':');
				if(p == -1) break;
				String k = line.substring(0, p);
				String v = line.substring(p+1).trim();
				//System.out.println("k=" + k);
				//System.out.println("v=" + v);
				String kk = k.toLowerCase();
				if(kk.equals("content-length")) len = v;
				if(kk.equals("transfer-encoding") && v.equals("chunked")) isChunkedEncoding = true;
				resultHeader.put(k, v);
			}
		}
		
		HTTPResult returningResult;
		
		if(isChunkedEncoding){
			
			if(event != null){
				returningResult = new HTTPResult(null, resultHeader, responseCode);
				WrappedInputStream wis = new WrappedInputStream() {
					//int chunkSize = 1;
					int capacityLeft = Integer.parseInt(readLine(is).trim(), 16);;
					@Override
					public int read(byte[] buf, int off, int len) throws IOException {
						if(capacityLeft == -1) return -1;
						if(capacityLeft == 0){
							readLine(is);//must be done here says specifications on Wikipedia. https://en.wikipedia.org/wiki/Chunked_transfer_encoding#Example
							capacityLeft = Integer.parseInt(readLine(is).trim(), 16);
							if(capacityLeft == 0){
								capacityLeft = -1;
								return -1;
							}
						}
						int l = is.read(buf, off, Math.min(len, capacityLeft));
						if(l == -1) {
							capacityLeft = -1;
						} else {
							capacityLeft -= l;
						}
						return l;
					}
				};

				event.onReceived(returningResult, wis);
				wis.readToEnd(DUMMY_BUFFER);
			} else {
				ByteArrayOutputStream baos = new  ByteArrayOutputStream();
				byte[] buf = new byte[4096];
				int chunkSize = 1;
				while(chunkSize != 0){
					chunkSize = Integer.parseInt(readLine(is).trim(), 16);
					copyBytes(is, baos, buf, chunkSize);
					readLine(is);//must be done here say specifications on wikipedia. https://en.wikipedia.org/wiki/Chunked_transfer_encoding#Example
				}
				returningResult = new HTTPResult(baos.toByteArray(), resultHeader, responseCode);
			}
			
		} else if(len == null){
			
			if(event != null){
				returningResult = new HTTPResult(null, resultHeader, responseCode);
				event.onReceived(returningResult, is);
			} else {
				ByteArrayOutputStream baos = new  ByteArrayOutputStream();
				int chr = -1;
				byte[] buffer = new byte[1024*32];
				try {
					BufferedInputStream bis = new BufferedInputStream(is, buffer.length * 4);
					while ((chr = bis.read(buffer)) != -1) {
						baos.write(buffer, 0, chr);
						//if(chr < buffer.length) break;
					}
				} catch(Exception e) {
				  if(!(e instanceof SocketTimeoutException)) e.printStackTrace();
				}
				returningResult = new HTTPResult(baos.toByteArray(), resultHeader, responseCode);
			}
			disabled = true;
			
		} else {
			
			if(event != null){
				returningResult = new HTTPResult(null, resultHeader, responseCode);
				final long lenVal = Long.parseLong(len);
				WrappedInputStream wis = new WrappedInputStream() {
					//int chunkSize = 1;
					long capacityLeft = lenVal;
					@Override
					public int read(byte[] buf, int off, int len) throws IOException {
						if(capacityLeft == -1) return -1;
						if(capacityLeft < len) {
							len = (int)capacityLeft;
							if(len == 0){
								capacityLeft = -1;
								return -1;
							}
						}
						int l = is.read(buf, off, len);
						if(l == -1) {
							capacityLeft = -1;
						} else {
							capacityLeft -= l;
						}
						return l;
					}
				};
				event.onReceived(returningResult, wis);
				wis.readToEnd(DUMMY_BUFFER);
			} else {
				byte[] arr = new byte[Integer.parseInt(len)];
				new DataInputStream(is).readFully(arr);
				returningResult = new HTTPResult(arr, resultHeader, responseCode);
			}
			
		}
		
		synchronized (this) {
			isInUse = false;
		}
		return returningResult;
	}

	public boolean canBeDeleted(long currentTime) {
		return (maxAge < currentTime) | disabled;
	}

	public void close() {
		disabled = true;
		try{
			socket.close();
		}catch(Exception e){}
	}
	
	private void resetAge(){
		maxAge = System.currentTimeMillis() + (120 * 1000);
	}
	public boolean isInUse(){
		if(disabled) return false;
		boolean b;
		synchronized (this) {
			b = isInUse;
		}
		return b;
	}

	public boolean tryClaim() {
		if(disabled) return false;
		boolean b = false;
		synchronized (this) {
			if(!isInUse){
				isInUse = b = true;
			}
		}
		return b;
	}

	public static abstract class WrappedInputStream extends InputStream{
		@Override
		public int read() throws IOException {
			byte[] b = new byte[1];
			if(read(b, 0, 1) != 1) return -1;
			return b[0] & 0xFF;
		}
		@Override
		public int read(byte[] buf) throws IOException {
			return read(buf, 0, buf.length);
		}
		
		@Override
		public abstract int read(byte[] buf, int off, int len) throws IOException;
		
		public void readToEnd(byte[] dummyBuffer) throws IOException {
			while(read(dummyBuffer, 0, dummyBuffer.length) != -1);
		}
	}
	
	private void copyBytes(InputStream is, ByteArrayOutputStream baos, byte[] bufPtr, int len) throws IOException {
		while(len > 0){
			int l = is.read(bufPtr, 0, Math.min(len, bufPtr.length));
			if(l == -1) throw new IOException("EOF");
			baos.write(bufPtr, 0, l);
			len -= l;
		}
		
	}

	private static String readLine(InputStream is) throws IOException {
		StringBuilder sb = new StringBuilder();
		int chr;
		while((chr = is.read()) != -1 && chr != '\n'){
			sb.append((char)chr);
		}
		String s = sb.toString();
		if(s.length() == 0) return s;
		if(s.charAt(s.length() - 1) == '\r') return s.substring(0, s.length() - 1);
		return s;
	}
	/*
	public boolean isClosed(){
		return socket.isClosed();
	}
	*/
	@Override
	protected void finalize() throws Throwable {
		//this.close();
		try {
			socket.close();
		} catch (IOException e) {}
		super.finalize();
	}
}
