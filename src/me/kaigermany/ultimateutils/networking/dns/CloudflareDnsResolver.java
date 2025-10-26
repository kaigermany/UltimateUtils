package me.kaigermany.ultimateutils.networking.dns;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;

public class CloudflareDnsResolver implements DnsResolver {
	@Override
	public InetAddress resolve(String hostname) throws UnknownHostException {
		System.out.println("resolve('"+hostname+"')");
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
		System.out.println(hostname + " -> " + ipText);
		byte[] ipv4Bytes = GoogleDnsResolver.textToNumericFormatV4(ipText);
		//System.out.println("ipv4Bytes = " + Arrays.toString(ipv4Bytes));
		if(ipv4Bytes == null) {
			return resolve(ipText);
		}
		return InetAddress.getByAddress(hostname, ipv4Bytes);
	}
	
	private static String runDnsRequest(String link) throws UnknownHostException {
		if(link.indexOf('.') == -1){
			throw new UnknownHostException("invalid dns name: " + link);
		}
		//alternative: "https://cloudflare-dns.com/dns-query?name="...
		@SuppressWarnings("deprecation")
		String resp = new String(GoogleDnsResolver.serverGet("https://1.1.1.1/dns-query?name=" + URLEncoder.encode(link) + "&type=A"));
		
		//resp = resp.split("\"Answer\"")[1].split("\"data\"\\:\"")[1].split("\"")[0];
		resp = resp.split("\"Answer\"")[1].split("\"data\"")[1].split("\"")[1];
		//System.out.println(resp);
		return resp;
	}
	
	public static byte[] serverGet(String a) {
		try {
			URL url = new URL(a);
			URLConnection conn = (URLConnection) url.openConnection();
			//conn.setRequestMethod("GET");
			//conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
			//conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:75.0) Gecko/20100101 Firefox/75.0");
			conn.setRequestProperty("Accept", "application/dns-json");
			//conn.setRequestProperty("Host", "1.1.1.1");

			//conn.setRequestProperty("Accept-Language", "de,en-US;q=0.7,en;q=0.3");
			BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int chr = -1;
			byte[] buffer = new byte[1024];
			try {
				while ((chr = bis.read(buffer)) > 0) baos.write(buffer, 0, chr);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			bis.close();
			return baos.toByteArray();
		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
