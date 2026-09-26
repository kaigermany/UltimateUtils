package me.kaigermany.ultimateutils.networking.smarthttp;

import java.io.IOException;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

public class SmartHTTP {
	private static int WATCHDOG_SLEEP_CYCLE = 60 * 1000;
	
	private static HashMap<String, HTTPServerGroup> clients = new HashMap<String, HTTPServerGroup>();
	
	/**
	 * Main method to do a web request.
	 * @param options Insert HTTPRequestOptions instance here with your configs.
	 * @return HTTPResult instance with all response bytes (unless stream listener option was defined in request)
	 * @throws IOException
	 */
	public static HTTPResult request(HTTPRequestOptions options) throws IOException {
		IOException firstException = null;
		for(int retrys = 0; retrys < options.getRetryCount(); retrys++){
			HTTPClient client = null;
			try{
				boolean isConnectionCloseRequested = checkForConectionClose(options.getHeaderFields());
				if(isConnectionCloseRequested){
					return new HTTPClient(options.getServer(), options.getPort(), options.getUseSSL(), options.getDisableCertificateCheck(), null, options.getProxy())
							.request(options.getPage(), options.getRequestMethod(), options.getHeaderFields(), options.getPostData(), options.getEvent(), options.areDefaultHeaderDisabled(), options.getTimeout());
				}
				client = getOrCreateConnection(options.getServer(), options.getPort(), options.getUseSSL(), options.getDisableCertificateCheck(), options.getMaxSocketCount(), options.getProxy());
				return client.request(options.getPage(), options.getRequestMethod(), options.getHeaderFields(), options.getPostData(), options.getEvent(), options.areDefaultHeaderDisabled(), options.getTimeout());
			}catch(IOException e){
				if(client != null) client.close();
				if(firstException == null) firstException = e;
			}
		}
		if(firstException == null) firstException = new IOException("unknown error: all retrys failed");
		throw firstException;
	}
	
	/**
	 * Optional way to reconfigure the watchdog delay.
	 * (if you know you have more long-living or many
	 * short-living peaks, then you can save some CPU time here.)
	 * <br/><br/> --- Use only if you are know what you're doing! ---
	 * @param millis Sleep time in milliseconds.
	 */
	public static void setWatchdogSleepCycle(int millis){
		if(millis <= 0) {
			throw new IllegalArgumentException("duration " + millis + " is too small, must be >= 1.");
		}
		WATCHDOG_SLEEP_CYCLE = millis;
	}
	
	/**
	 * Getter to get statistical connection counts. <br/>
	 * Note: high call rates may throttle the overall 
	 * performance, caused by internal mutex usages.
	 * @return Sum of all active connection handled here.
	 */
	public static int getActiveConnectionCount(){
		int count = 0;
		synchronized (clients) {
			for(HTTPServerGroup group : clients.values()){
				count += group.getNumActiveConnections();
			}
		}
		return count;
	}
	
	private static HTTPClient getOrCreateConnection(String server, int port, boolean ssl, boolean disableCertificateCheck, int maxSocketCount, Proxy proxy) throws UnknownHostException, IOException {
		if(maxSocketCount <= 0) return null;
		
		String searchKey = server + "&" + port + "&" + ssl + "&" + disableCertificateCheck + "&" + proxy;
		while(true){
			HTTPServerGroup group;
			synchronized (clients) {
				group = clients.computeIfAbsent(searchKey, k->new HTTPServerGroup());
			}
			if(group.getNumActiveConnections() < maxSocketCount){
				HTTPClient clientInstance = group.getOrCreateClient(server, port, ssl, disableCertificateCheck, proxy);
				if(clientInstance != null){
					tryStartWatchDog();
					return clientInstance;
				}
			}
			sleep(50);
		}
	}
	
	private static void sleep(int ms){
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ignored) {}
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
		}, "SmartHTTP Cleanup Watchdog");
		t.setDaemon(true);
		t.start();
	}
}
