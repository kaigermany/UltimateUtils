package me.kaigermany.ultimateutils.networking.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import me.kaigermany.ultimateutils.StringUtils;

public class CloudflareDnsResolver extends DnsResolver {
	@Override
	public InetAddress resolve(String hostname) throws UnknownHostException {
		//hardcoded self-resolving.
		if (hostname.equals("cloudflare-dns.com")) {
			return InetAddress.getByAddress(hostname, new byte[]{1, 1, 1, 1});
		}
		if (hostname.equals("one.one.one.one")) {
			return InetAddress.getByAddress(hostname, new byte[]{1, 1, 1, 1});
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
			String resp = new String(serverGet("https://1.1.1.1/dns-query?name=" + encodeUrl(domain) + "&type=A"), StandardCharsets.UTF_8);
			
			//find location of Answer parameter
			resp = resp.substring(resp.indexOf("\"Answer\""));
			//extract data parameter
			return StringUtils.splitAndKeepMiddle(resp, "\"data\":\"", "\"");
		}catch(Exception e){
			throw (UnknownHostException)new UnknownHostException(domain).initCause(e);
		}
	}
}
