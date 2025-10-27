package me.kaigermany.ultimateutils.networking.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import me.kaigermany.ultimateutils.data.json.JSONObject;

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
		
		return runDnsRequest(hostname);
	}
	
	private InetAddress runDnsRequest(String hostname) throws UnknownHostException {
		if(hostname.indexOf('.') == -1){
			throw new UnknownHostException("invalid hostname: " + hostname);
		}
		
		try{
			String resp = new String(serverGet("https://1.1.1.1/dns-query?name=" + encodeUrl(hostname) + "&type=A"), StandardCharsets.UTF_8);
			
			String ipText = JSONObject.parse(resp).getJSONArray("Answer").getJSONObject(0).getString("data");
			byte[] ipv4Bytes = textToNumericFormatV4(ipText);
			
			if(ipv4Bytes == null){//not a IP? than its a domain-pointer.
				return resolve(ipText);
			}
			
			return InetAddress.getByAddress(hostname, ipv4Bytes);
		}catch(Exception e){
			throw (UnknownHostException)new UnknownHostException(hostname).initCause(e);
		}
	}
}
