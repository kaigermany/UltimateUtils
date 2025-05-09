package me.kaigermany.ultimateutils.networking.smarthttp;

import java.io.IOException;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

public class SmartHTTP {
	private static int NUM_MAX_CONNECTIONS_PER_SERVER = 10;
	private static int WATCHDOG_SLEEP_CYCLE = 60 * 1000;
	
	private static HashMap<String, HTTPServerGroup> clients = new HashMap<String, HTTPServerGroup>();

	@Deprecated
	public static HTTPResult request(String url, String requestMethod, HashMap<String, String> headerFields, byte[] postData) throws IOException {
		String[] urlElements = parseUrl(url);
		int port;
		boolean ssl = urlElements[0] != null && urlElements[0].equals("https");
		if(urlElements[2] == null) {
			port = ssl ? 443 : 80;
		} else {
			port = Integer.parseInt(urlElements[2]);
		}
		return request(urlElements[1], port, urlElements[3], requestMethod, headerFields, postData, NUM_MAX_CONNECTIONS_PER_SERVER, ssl, false, null);
	}
	@Deprecated
	public static HTTPResult request(String url, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount) throws IOException {
		String[] urlElements = parseUrl(url);
		int port;
		boolean ssl = urlElements[0] != null && urlElements[0].equals("https");
		if(urlElements[2] == null) {
			port = ssl ? 443 : 80;
		} else {
			port = Integer.parseInt(urlElements[2]);
		}
		return request(urlElements[1], port, urlElements[3], requestMethod, headerFields, postData, maxSocketCount, ssl, false, null);
	}
	@Deprecated
	public static HTTPResult request(String url, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount, boolean ssl) throws IOException {
		String[] urlElements = parseUrl(url);
		if(urlElements[0] != null && !urlElements[0].equals("https") && ssl){
			throw new IOException("if ssl = true then the protocol is forced to use https");
		}
		int port;
		if(urlElements[2] == null) {
			port = ssl ? 443 : 80;
		} else {
			port = Integer.parseInt(urlElements[2]);
		}
		return request(urlElements[1], port, urlElements[3], requestMethod, headerFields, postData, maxSocketCount, ssl, false, null);
	}
	@Deprecated
	public static HTTPResult request(String url, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount, boolean ssl, boolean disableCertificateCheck) throws IOException {
		String[] urlElements = parseUrl(url);
		if(urlElements[0] != null && !urlElements[0].equals("https") && ssl){
			throw new IOException("if ssl = true then the protocol is forced to use https");
		}
		int port;
		if(urlElements[2] == null) {
			port = ssl ? 443 : 80;
		} else {
			port = Integer.parseInt(urlElements[2]);
		}
		return request(urlElements[1], port, urlElements[3], requestMethod, headerFields, postData, maxSocketCount, ssl, disableCertificateCheck, null);
	}
	@Deprecated
	public static HTTPResult request(String server, int port, String page, String requestMethod, HashMap<String, String> headerFields, byte[] postData) throws IOException {
		return request(server, port, page, requestMethod, headerFields, postData, NUM_MAX_CONNECTIONS_PER_SERVER);
	}
	@Deprecated
	public static HTTPResult request(String server, int port, String page, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount) throws IOException {
		return request(server, port, page, requestMethod, headerFields, postData, maxSocketCount, port == 443);
	}
	@Deprecated
	public static HTTPResult request(String server, int port, String page, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount, boolean ssl) throws IOException {
		return request(server, port, page, requestMethod, headerFields, postData, maxSocketCount, ssl, false);
	}
	@Deprecated
	public static HTTPResult request(String server, int port, String page, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount, boolean ssl, boolean disableCertificateCheck) throws IOException {
		return request(server, port, page, requestMethod, headerFields, postData, maxSocketCount, ssl, disableCertificateCheck, null);
	}
	@Deprecated
	public static HTTPResult request(String url, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount, boolean ssl, boolean disableCertificateCheck, HTTPResultEvent event) throws IOException {
		String[] urlElements = parseUrl(url);
		if(urlElements[0] != null && !urlElements[0].equals("https") && ssl){
			throw new IOException("if ssl = true then the protocol is forced to use https");
		}
		int port;
		if(urlElements[2] == null) {
			port = ssl ? 443 : 80;
		} else {
			port = Integer.parseInt(urlElements[2]);
		}
		return request(urlElements[1], port, urlElements[3], requestMethod, headerFields, postData, maxSocketCount, ssl, disableCertificateCheck, event);
	}
	
	public static HTTPResult request(HTTPRequestOptions options) throws IOException {
		return request(options.getServer(), options.getPort(), options.getPage(), options.getRequestMethod(),
				options.getHeaderFields(), options.getPostData(), options.getMaxSocketCount(),
				options.getUseSSL(), options.getDisableCertificateCheck(), options.getEvent(),
				options.getRetryCount(), options.areDefaultHeaderDisabled(), options.getProxy());
	}
	@Deprecated
	public static HTTPResult request(String server, int port, String page, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount, boolean ssl, boolean disableCertificateCheck, HTTPResultEvent event) throws IOException {
		return request(server, port, page, requestMethod, headerFields, postData, maxSocketCount, ssl, disableCertificateCheck, event, 3, false, null);
	}
	
	public static HTTPResult request(String server, int port, String page, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount, boolean ssl, boolean disableCertificateCheck, HTTPResultEvent event, int numRetrys, boolean noDefaultHeader, Proxy proxy) throws IOException {
		IOException exception = null;
		for(int retrys = 0; retrys < numRetrys; retrys++){
			HTTPClient client = null;
			try{
				boolean isConnectionCloseRequested = checkForConectionClose(headerFields);
				if(isConnectionCloseRequested){
					return new HTTPClient(server, port, ssl, disableCertificateCheck, null, proxy).request(page, requestMethod, headerFields, postData, event, noDefaultHeader);
				}
				client = getOrCreateConnection(server, port, ssl, disableCertificateCheck, maxSocketCount, proxy);
				return client.request(page, requestMethod, headerFields, postData, event, noDefaultHeader);
			}catch(IOException e){
				if(client != null) client.close();
				if(exception == null) exception = e;
				//e.printStackTrace();
				//System.out.println("retry#" + (retrys + 1));
			}
		}
		if(exception == null) exception = new IOException("unknown error: all retrys failed");
		throw exception;
	}
	
	private static HTTPClient getOrCreateConnection(String server, int port, boolean ssl, boolean disableCertificateCheck, int maxSocketCount, Proxy proxy) throws UnknownHostException, IOException {
		if(maxSocketCount <= 0) return null;
		
		String searchKey = server + "&" + port + "&" + ssl + "&" + disableCertificateCheck;
		//long printTimeout = (30 * 1000) / 50;
		while(true){
			synchronized (clients) {
				HTTPServerGroup group = clients.computeIfAbsent(searchKey, k->new HTTPServerGroup());
				if(group.getNumActiveConnections() < maxSocketCount){
					HTTPClient clientInstance = group.getOrCreateClient(server, port, ssl, disableCertificateCheck, proxy);
					if(clientInstance != null){
						tryStartWatchDog();
						return clientInstance;
					}
				}
			}
			
			sleep(50);
			/*
			printTimeout--;
			if(printTimeout < 0){
				synchronized (clients) {
					HTTPServerGroup group = clients.get(searchKey);
					System.out.println("[SmartHTTP debug dump] potential timeout for key '"
						+ searchKey + "', numConnections = " + group.getNumActiveConnections()
						+ ", HTTPServerGroup: " + group.toString());
				}
			}
			*/
		}
	}
	
	private static void sleep(int ms){
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static boolean checkForConectionClose(HashMap<String, String> map){
		if(map == null) return false;
		for(Map.Entry<String, String> e : map.entrySet()){
			if(e.getKey().toLowerCase().equals("connection") && e.getValue().toLowerCase().equals("close")){
				return true;
			}
		}
		return false;
	}
	
	
	private static volatile SmartHTTP instance;
	
	private static void tryStartWatchDog() {
		synchronized (clients) {
			if(instance == null) instance = new SmartHTTP();
		}
	}
	
	private SmartHTTP(){
		Thread t = new Thread(new Runnable(){
			public void run(){
				while(true){
					try {
						Thread.sleep(WATCHDOG_SLEEP_CYCLE);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					long currentTime = System.currentTimeMillis();
					
					synchronized (clients) {
						LinkedList<String> todoDelete = new LinkedList<String>();
						for(Entry<String, HTTPServerGroup> group : clients.entrySet()){
							if(group.getValue().cleanup(currentTime)){
								todoDelete.add(group.getKey());
							}
						}
						if(todoDelete.size() > 0){
							for(String k : todoDelete){
								clients.remove(k);
							}
						}
						
						if(clients.isEmpty()){
							instance = null;
							return;
						}
					}
				}
			}
		}, "SmartHTTP Watchdog");
		t.setDaemon(true);
		t.start();
	}
	
	
	
	
	
	
	
	private static String[] parseUrl(String url){
		String protocol = null;
		int p = url.indexOf("://");
		if(p != -1) {
			protocol = url.substring(0, p);
			url = url.substring(p + 3);
		}
		p = url.indexOf('/');
		String serverName;
		String targetPath;
		if(p == -1){
			serverName = url;
			targetPath = "/";
		} else {
			serverName = url.substring(0, p);
			targetPath = url.substring(p);
		}
		
		String ip;
		String port = null;
		p = serverName.lastIndexOf(':');
		int p2 = serverName.lastIndexOf(']');
		if(p2 > p) {//mismatch-test:
			//pattern: "[ip:0::v6]:port"
			//so, if ':' is last found
			//before ']' then it's part of IPv6 and not the port
			p = -1;
		}
		if(p == -1){
			ip = serverName;
		} else {
			ip = serverName.substring(0, p);
			port = serverName.substring(p + 1);
		}
		
		return new String[]{protocol, ip, port, targetPath};
	}
}
