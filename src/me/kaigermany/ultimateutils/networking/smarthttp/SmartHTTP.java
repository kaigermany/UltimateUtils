package me.kaigermany.ultimateutils.networking.smarthttp;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
//TODO test
public class SmartHTTP {
	private static int NUM_MAX_CONNECTIONS_PER_SERVER = 10;
	private static HashMap<String, ArrayList<HTTPClient>> clientInstances = new HashMap<String, ArrayList<HTTPClient>>();
	private static HashMap<String, HTTPClientCount> clientInstanceCounts = new HashMap<String, HTTPClientCount>();
	private static int WATCHDOG_SLEEP_CYCLE = 60 * 1000;
	
	private static class HTTPClientCount {
		int value;
	}
	
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
				options.getUseSSL(), options.getDisableCertificateCheck(), options.getEvent());
	}
	
	public static HTTPResult request(String server, int port, String page, String requestMethod, HashMap<String, String> headerFields, byte[] postData, int maxSocketCount, boolean ssl, boolean disableCertificateCheck, HTTPResultEvent event) throws IOException {
		IOException exception = null;
		for(int retrys = 0; retrys < 3; retrys++){
			try{
				boolean isConnectionCloseRequested = checkForConectionClose(headerFields);
				if(isConnectionCloseRequested){
					return new HTTPClient(server, port, ssl, disableCertificateCheck).request(page, requestMethod, headerFields, postData, event);
				}
				HTTPClient client = getOrCreateConnection(server, port, ssl, disableCertificateCheck, maxSocketCount);
				return client.request(page, requestMethod, headerFields, postData, event);
			}catch(IOException e){
				if(exception == null) exception = e;
				System.out.println("retry#" + (retrys + 1));
			}
		}
		if(exception == null) exception = new IOException("unknown error: all retrys failt");
		throw exception;
	}
	
	private static HTTPClient getOrCreateConnection(String server, int port, boolean ssl, boolean disableCertificateCheck, int maxSocketCount) throws UnknownHostException, IOException {
		String searchKey = server + "&" + port + "&" + ssl + "&" + disableCertificateCheck;
		
		HTTPClient clientInstance = null;
		int count = 0;
		synchronized (clientInstances) {
			count = clientInstanceCounts.getOrDefault(searchKey, new HTTPClientCount()).value;
			ArrayList<HTTPClient> list = clientInstances.get(searchKey);
			if(list != null){
				for(HTTPClient client : list){
					if(client.tryClaim()) {
						clientInstance = client;
						//clientInstanceCounts.get(searchKey).value++;
						break;
					}
				}
			}
		}
		if(clientInstance != null){
			return clientInstance;
		}
		
		if(count == maxSocketCount){
			sleep(50);
			boolean loop = true;
			while(loop){
				synchronized (clientInstances) {
					HTTPClientCount counterInstance = clientInstanceCounts.get(searchKey);
					if(counterInstance == null) clientInstanceCounts.put(searchKey, counterInstance = new HTTPClientCount());
					count = counterInstance.value;
					ArrayList<HTTPClient> list = clientInstances.get(searchKey);
					if(list != null){
						for(HTTPClient client : list){
							if(client.tryClaim()) {
								clientInstance = client;
								loop = false;
								break;
							}
						}
					}
					
					if(clientInstanceCounts.get(searchKey).value < maxSocketCount){
						loop = false;
					}
				}
				if(clientInstance != null){
					return clientInstance;
				}
				sleep(100);
			}
		}
		
		clientInstance = new HTTPClient(server, port, ssl, disableCertificateCheck);
		
		synchronized (clientInstances) {
			ArrayList<HTTPClient> list = clientInstances.get(searchKey);
			if(list == null){
				list = new ArrayList<HTTPClient>(maxSocketCount);
				clientInstances.put(searchKey, list);
				clientInstanceCounts.put(searchKey, new HTTPClientCount());
			}
			list.add(clientInstance);
			clientInstanceCounts.get(searchKey).value++;
			tryStartWatchDog();
		}
		
		return clientInstance;
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
					synchronized (clientInstances) {
						for(String k : new ArrayList<String>(clientInstances.keySet())){
							ArrayList<HTTPClient> list = clientInstances.get(k);
							if(list == null || list.size() == 0){
								clientInstances.remove(k);
								continue;
							}
							
							int canBeDeletedCount = 0;
							for(HTTPClient client : list){
								if(client.canBeDeleted(currentTime)) canBeDeletedCount++;
							}
							
							{
								HTTPClientCount counterInstance = clientInstanceCounts.get(k);
								if(counterInstance != null){
									counterInstance.value -= canBeDeletedCount;
									if(counterInstance.value == 0){
										clientInstanceCounts.remove(k);
									}
									counterInstance = null;
								}
							}
							
							if(canBeDeletedCount == list.size()){//drop list
								clientInstances.remove(k);
								for(HTTPClient client : list){
									client.close();
								}
							} else if(canBeDeletedCount >= list.size() / 2){//shrink list
								ArrayList<HTTPClient> newList = new ArrayList<HTTPClient>((list.size() - canBeDeletedCount) + 1);
								for(HTTPClient client : list){
									if(client.canBeDeleted(currentTime)) {
										client.close();
									} else {
										newList.add(client);
									}
								}
								clientInstances.put(k, newList);
							} else if(canBeDeletedCount > 0){//pick single elements and remove them
								for(int i=list.size(); i>=0; i--){
									if(list.get(i).canBeDeleted(currentTime)) {
										list.remove(i).close();
									}
								}
							}
						}
						onExit = clientInstances.size() == 0;
						if(onExit){
							instance = null;
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
