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
	
	/**
	 * returns the stored HTTP response code.
	 * @return HTTP response code
	 */
	public int getResponseCode(){
		return responseCode;
	}
	
	/**
	 * returns the HTTP body. If a stream was ordered, the value is null.
	 * @return content
	 */
	public byte[] getData(){
		return content;
	}
	
	/**
	 * returns the HTTP body. If a stream was ordered, the value is null.
	 * @return HTTP response header
	 */
	public HashMap<String, String> getHeaders(){
		return header;
	}
	
	@Override
	public String toString(){
		return "{ResponseCode: " + responseCode + ", Content-Length: " + 
			(content == null ? "none (Stream-Mode)" : String.valueOf(content.length)) +
			", Header: " + header + "}";
	}
}
