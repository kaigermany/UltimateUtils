package me.kaigermany.ultimateutils.networking.smarthttp;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SmartHTTP {
	private static int NUM_MAX_CONNECTIONS_PER_SERVER = 10;
	//private static HashMap<String, ArrayList<HTTPClient>> clientInstances = new HashMap<String, ArrayList<HTTPClient>>();
	//private static HashMap<String, HTTPClientCount> clientInstanceCounts = new HashMap<String, HTTPClientCount>();
	private static int WATCHDOG_SLEEP_CYCLE = 60 * 1000;
	private static HashMap<String, LinkedList<HTTPClient>> clientInstancesActive = new HashMap<String, LinkedList<HTTPClient>>();
	private static HashMap<String, LinkedList<HTTPClient>> clientInstancesIdle = new HashMap<String, LinkedList<HTTPClient>>();
	private static HashMap<HTTPClient, String> instanceTable = new HashMap<HTTPClient, String>();
	/*
	private static class HTTPClientCount {
		int value;
	}
	*/
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
	public static HTTPResult request(String server, int port, String page, String requestMethod, HashMap<String, String> headerFields, byte[] postData) throws IOException {
		return request(server, port, page, requestMethod, headerFields, postData, NUM_MAX_CONNECTIONS_PER_SERVER);
	}
	public static HTTPResult request(String server, int port, String page, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount) throws IOException {
		return request(server, port, page, requestMethod, headerFields, postData, maxSocketCount, port == 443);
	}
	public static HTTPResult request(String server, int port, String page, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount, boolean ssl) throws IOException {
		return request(server, port, page, requestMethod, headerFields, postData, maxSocketCount, ssl, false);
	}
	public static HTTPResult request(String server, int port, String page, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount, boolean ssl, boolean disableCertificateCheck) throws IOException {
		return request(server, port, page, requestMethod, headerFields, postData, maxSocketCount, ssl, disableCertificateCheck, null);
	}
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
				options.getUseSSL(), options.getDisableCertificateCheck(), options.getEvent(), options.getRetryCount(), options.areDefaultHeaderDisabled());
	}
	public static HTTPResult request(String server, int port, String page, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount, boolean ssl, boolean disableCertificateCheck, HTTPResultEvent event) throws IOException {
		return request(server, port, page, requestMethod, headerFields, postData, maxSocketCount, ssl, disableCertificateCheck, event, 3, false);
	}
	public static HTTPResult request(String server, int port, String page, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount, boolean ssl, boolean disableCertificateCheck, HTTPResultEvent event, int numRetrys, boolean noDefaultHeader) throws IOException {
		IOException exception = null;
		for(int retrys = 0; retrys < numRetrys; retrys++){
			HTTPClient client = null;
			try{
				boolean isConnectionCloseRequested = checkForConectionClose(headerFields);
				if(isConnectionCloseRequested){
					return new HTTPClient(server, port, ssl, disableCertificateCheck).request(page, requestMethod, headerFields, postData, event, noDefaultHeader);
				}
				client = getOrCreateConnection(server, port, ssl, disableCertificateCheck, maxSocketCount);
				return client.request(page, requestMethod, headerFields, postData, event, noDefaultHeader);
			}catch(IOException e){
				if(client != null) client.close();
				if(exception == null) exception = e;
				e.printStackTrace();
				System.out.println("retry#" + (retrys + 1));
			}
		}
		if(exception == null) exception = new IOException("unknown error: all retrys failt");
		throw exception;
	}
	
	private static HTTPClient getOrCreateConnection(String server, int port, boolean ssl, boolean disableCertificateCheck, int maxSocketCount) throws UnknownHostException, IOException {
		if(maxSocketCount <= 0) return null;
		
		String searchKey = server + "&" + port + "&" + ssl + "&" + disableCertificateCheck;
		
		while(true){
			synchronized (clientInstancesActive) {
				LinkedList<HTTPClient> active = clientInstancesActive.computeIfAbsent(searchKey, k->new LinkedList<HTTPClient>());
				LinkedList<HTTPClient> idle = clientInstancesIdle.computeIfAbsent(searchKey, k->new LinkedList<HTTPClient>());
				if(active.size() < maxSocketCount){
					if(idle.size() == 0) {
						
							HTTPClient clientInstance = new HTTPClient(server, port, ssl, disableCertificateCheck);//TODO potential to freeze other threads here!
							active.add(clientInstance);
							instanceTable.put(clientInstance, searchKey);
							tryStartWatchDog();
							return clientInstance;
						//}
					} else {
						//if(active.size() < maxSocketCount){
							HTTPClient clientInstance = idle.remove();
							if(!clientInstance.tryClaim()){
								System.err.println("WARNING: Invalid HTTPClient instance found!");
								instanceTable.remove(clientInstance);
							} else {
								active.add(clientInstance);
								return clientInstance;
							}
						//}
					}
				}
					
			}
			
			sleep(50);
		}
	}
	
	public static void markInstanceAsUnused(HTTPClient client){
		synchronized (clientInstancesActive) {
			String searchKey = instanceTable.get(client);
			LinkedList<HTTPClient> active = clientInstancesActive.computeIfAbsent(searchKey, k->new LinkedList<HTTPClient>());
			LinkedList<HTTPClient> idle = clientInstancesIdle.computeIfAbsent(searchKey, k->new LinkedList<HTTPClient>());
			active.remove(client);
			idle.add(client);
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
	
	
	private static SmartHTTP instance;
	private static void tryStartWatchDog() {
		if(instance == null) instance = new SmartHTTP();
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
					boolean onExit = false;
					long currentTime = System.currentTimeMillis();
					
					synchronized (clientInstancesActive) {
						LinkedList<HTTPClient> todoDelete = new LinkedList<HTTPClient>();
						for(LinkedList<HTTPClient> clients : clientInstancesIdle.values()){
							for(HTTPClient c : clients){
								if(c.canBeDeleted(currentTime)){
									instanceTable.remove(c);
									todoDelete.add(c);
								}
							}
							if(todoDelete.size() > 0){
								clients.removeAll(todoDelete);
								clients.clear();
							}
						}
					}
					if(onExit) break;
				}
			}
		});
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
