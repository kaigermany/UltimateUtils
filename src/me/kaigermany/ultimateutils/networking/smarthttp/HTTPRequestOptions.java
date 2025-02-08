package me.kaigermany.ultimateutils.networking.smarthttp;

import java.util.HashMap;

public class HTTPRequestOptions {
	private final String server;
	private final int port;
	private final String page;
	
	private String requestMethod = "GET";
	private HashMap<String, String> headerFields;
	private byte[] postData;
	private int maxSocketCount = 10;
	private boolean useSSL;
	private boolean disableCertificateCheck;
	private HTTPResultEvent event;
	private int numRetrys = 3;
	
	public HTTPRequestOptions(String url){
		String[] urlElements = parseUrl(url);
		int port;
		if(urlElements[0] != null){
			useSSL = urlElements[0].equals("https");
		}
		if(urlElements[2] == null) {
			port = useSSL ? 443 : 80;
		} else {
			port = Integer.parseInt(urlElements[2]);
			if(port == 443) {
				useSSL = true;
			}
		}
		this.server = urlElements[1];
		this.port = port;
		this.page = urlElements[3];
	}
	
	public HTTPRequestOptions(String server, int port, String page){
		this.server = server;
		this.port = port;
		this.page = page;
	}
	
	public String getServer(){
		return server;
	}
	
	public int getPort(){
		return port;
	}
	
	public String getPage(){
		return page;
	}
	
	public HTTPRequestOptions setRequestMethod(String requestMethod){
		this.requestMethod = requestMethod;
		return this;
	}
	
	public String getRequestMethod(){
		return requestMethod;
	}
	
	public HTTPRequestOptions setRequestParameters(HashMap<String, String> headerFields){
		this.headerFields = headerFields;
		return this;
	}
	
	public HashMap<String, String> getHeaderFields(){
		return headerFields;
	}
	
	public HTTPRequestOptions setPostData(byte[] postData){
		this.postData = postData;
		return this;
	}
	
	public byte[] getPostData(){
		return postData;
	}
	
	public HTTPRequestOptions setMaxSocketCount(int maxSocketCount){
		this.maxSocketCount = maxSocketCount;
		return this;
	}
	
	public int getMaxSocketCount(){
		return maxSocketCount;
	}
	
	public HTTPRequestOptions setUseSSL(boolean useSSL){
		this.useSSL = useSSL;
		return this;
	}
	
	public boolean getUseSSL(){
		return useSSL;
	}
	
	public HTTPRequestOptions setDisableCertificateCheck(boolean disableCertificateCheck){
		this.disableCertificateCheck = disableCertificateCheck;
		return this;
	}
	
	public boolean getDisableCertificateCheck(){
		return disableCertificateCheck;
	}
	
	public HTTPRequestOptions setEvent(HTTPResultEvent event){
		this.event = event;
		return this;
	}
	
	public HTTPResultEvent getEvent(){
		return event;
	}
	
	public HTTPRequestOptions setRetryCount(int count){
		if(count < 1) throw new IllegalArgumentException("retries must be at least 1 or higher");
		this.numRetrys = count;
		return this;
	}
	
	public HTTPRequestOptions setNoRetrys(){
		this.numRetrys = 1;
		return this;
	}

	public int getRetryCount() {
		return numRetrys;
	}
	
	public HTTPRequestOptions dublicate(){
		HTTPRequestOptions copy = new HTTPRequestOptions(server, port, page);
		copy.requestMethod = this.requestMethod;
		copy.headerFields = this.headerFields;
		copy.postData = this.postData;
		copy.maxSocketCount = this.maxSocketCount;
		copy.useSSL = this.useSSL;
		copy.disableCertificateCheck = this.disableCertificateCheck;
		copy.event = this.event;
		return copy;
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
