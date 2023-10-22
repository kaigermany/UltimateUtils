package me.kaigermany.ultimateutils.networking.smarthttp;

import java.util.HashMap;

public class HTTPResult {
	private final byte[] content;
	private final HashMap<String, String> header;
	private final int responseCode;
	
	public HTTPResult(byte[] content, HashMap<String, String> header, int responseCode){
		this.content = content;
		this.header = header;
		this.responseCode = responseCode;
	}
	
	public int getResponseCode(){
		return responseCode;
	}
	
	public byte[] getData(){
		return content;
	}
	
	public HashMap<String, String> getHeaders(){
		return header;
	}
	
	public String toString(){
		return "{ResponseCode: " + responseCode + ", Content-Length: " + (content == null ? -1 : content.length) + ", Header: " + header + "}";
	}
}
