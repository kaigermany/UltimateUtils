package me.kaigermany.ultimateutils.networking.dns;

import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import me.kaigermany.ultimateutils.StringUtils;

public class GoogleDnsResolver extends DnsResolver {

	@Override
	public InetAddress resolve(String hostname) throws UnknownHostException {
		//hardcoded self-resolving.
		if (hostname.equals("dns.google.com")) {
			return InetAddress.getByAddress(hostname, new byte[]{8, 8, 4, 4});
		}
		if (hostname.equals("localhost")) {
			return InetAddress.getByAddress(hostname, new byte[]{127, 0, 0, 1});
		}
		
		String ipText = runDnsRequest(hostname);
		byte[] ipv4Bytes = textToNumericFormatV4(ipText);
		if(ipv4Bytes == null) {
			return resolve(ipText);
		}
		return InetAddress.getByAddress(hostname, ipv4Bytes);
	}
	
	private static String runDnsRequest(String domain) throws UnknownHostException {
		if(domain.indexOf('.') == -1){
			throw new UnknownHostException("invalid dns name: " + domain);
		}
		
		try{
			@SuppressWarnings("deprecation")
			String resp = new String(serverGet("https://dns.google.com/resolve?name=" + URLEncoder.encode(domain) + "&type=A"), StandardCharsets.UTF_8);
			
			//find location of Answer parameter
			resp = resp.substring(resp.indexOf("\"Answer\""));
			//extract data parameter
			return StringUtils.splitAndKeepMiddle(resp, "\"data\":\"", "\"");
		}catch(Exception e){
			throw (UnknownHostException)new UnknownHostException(domain).initCause(e);
		}
	}
}
