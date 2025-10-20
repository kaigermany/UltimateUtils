package me.kaigermany.ultimateutils.networking.httpserver;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import me.kaigermany.ultimateutils.sync.thread.ThreadLock;

public class HttpServer {
	private final ServerSocket serverSocket;
	private volatile boolean alive = true;
	private final ThreadLock shutdownLock = new ThreadLock();
	private LinkedList<RequestConsumer> listener = new LinkedList<RequestConsumer>();
	private int threadPriority;
	private final Thread serverThread;
	
	public HttpServer(int port) throws IOException {
		this.serverSocket = new ServerSocket(port);
		this.serverSocket.setSoTimeout(10000);
		this.shutdownLock.lock();
		serverThread = new Thread(new Runnable(){
			@Override
			public void run(){
				acceptLoop();
			}
		}, "HttpServer_" + port);
		serverThread.start();
	}
	
	public void stop(){
		alive = false;
	}
	
	public void stopAndAwait(){
		alive = false;
		this.shutdownLock.enterBlock();
	}
	
	public void attachListener(RequestConsumer handler){
		listener.add(handler);
	}
	
	public void detachListener(RequestConsumer handler){
		listener.remove(handler);
	}
	
	public void detachAllListener(){
		listener.clear();
	}
	
	private void acceptLoop(){
		try{
			while(alive){
				try{
					Thread t = new Thread(new SocketInstance(serverSocket.accept(), listener));
					t.setDaemon(true);
					t.setPriority(threadPriority);
					t.start();
				}catch(SocketTimeoutException ignored){}
			}
		}catch(Throwable anyError){
			anyError.printStackTrace();
		}
		this.shutdownLock.unlock();
	}
	
	private static final class SocketInstance implements Runnable {
		private final Socket socket;
		private final LinkedList<RequestConsumer> listener;
		private String requestMethod;
		private String requestPath;
		private String requestProtocolVersion;
		private Map<String, List<String>> requestHeaders;
		private InputStream is;
		private OutputStream os;
		
		public SocketInstance(Socket socket, LinkedList<RequestConsumer> listener) {
			System.out.println("ACCEPT()");
			this.socket = socket;
			this.listener = new LinkedList<RequestConsumer>(listener);
		}

		@Override
		public void run() {
			try{
				is = this.socket.getInputStream();
				os = this.socket.getOutputStream();
				readHeader();
				for(RequestConsumer handler : listener){
					if(handler.accept(requestMethod, requestPath, requestProtocolVersion, requestHeaders, is, os)){
						break;
					}
				}
			}catch(Throwable anyError){
				anyError.printStackTrace();
			}
		}
		
		private void readHeader() throws IOException {
			requestHeaders = new HashMap<String, List<String>>();
			ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(128);
			
			{
				String headLine = readLine(is, lineBuffer);
				System.out.println("headLine = " + headLine);
				int left = headLine.indexOf(' ');
				int right = headLine.lastIndexOf(' ');
				if((left | right) == -1 || left == right){
					throw new IOException("incorrect HTTP headline");
				}
				
				requestMethod = headLine.substring(0, left);
				requestPath = headLine.substring(left + 1, right);
				requestProtocolVersion = headLine.substring(right + 1);
			}
			
			String line;
			while((line = readLine(is, lineBuffer)).length() > 2){//"x:y"
				int separator = line.indexOf(':');
				String key = line.substring(0, separator).trim();
				String val = line.substring(separator + 1).trim();
				List<String> values = requestHeaders.get(key);
				if(values == null) {
					requestHeaders.put(key, values = new ArrayList<String>(10));
				}
				values.add(val);
			}
		}
		
		private static String readLine(InputStream is, ByteArrayOutputStream lineBuffer) throws IOException {
			int chr;
			lineBuffer.reset();
			while(true){
				if((chr = is.read()) == -1){
					throw new EOFException();
				}
				if(chr == '\n'){
					break;
				}
				lineBuffer.write(chr);
			}
			return new String(lineBuffer.toByteArray(), StandardCharsets.UTF_8).trim();
			
		}
	}

	public HttpServer setPriority(int threadPriority) {
		this.threadPriority = threadPriority;
		serverThread.setPriority(threadPriority);
		return this;
	}
}
