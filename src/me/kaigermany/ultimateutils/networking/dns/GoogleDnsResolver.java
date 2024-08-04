package me.kaigermany.ultimateutils.networking.dns;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;

public class GoogleDnsResolver implements DnsResolver{

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
		System.out.println(hostname + " -> " + ipText);
		return InetAddress.getByAddress(hostname, textToNumericFormatV4(ipText));
	}
	
	private static String runDnsRequest(String link) {
		@SuppressWarnings("deprecation")
		String resp = new String(serverGet("https://dns.google.com/resolve?name=" + URLEncoder.encode(link) + "&type=A"));
		//System.out.println(resp);
		resp = resp.split("\"Answer\"")[1].split("\"data\"\\:\"")[1].split("\"")[0];
		//System.out.println(resp);
		return resp;
	}
	
	public static byte[] serverGet(String a) {
		try {
			URL url = new URL(a);
			URLConnection conn = (URLConnection) url.openConnection();
			//conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
			//conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:75.0) Gecko/20100101 Firefox/75.0");
			conn.setRequestProperty("Accept", "application/dns-json");
			//conn.setRequestProperty("Host", "dns.google.com");

			conn.setRequestProperty("Accept-Language", "de,en-US;q=0.7,en;q=0.3");
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
	
	public static byte[] textToNumericFormatV4(String src) { //src: sun.net.util.IPAddressUtil
		byte[] res = new byte[4];

		long tmpValue = 0;
		int currByte = 0;
		boolean newOctet = true;

		int len = src.length();
		if (len == 0 || len > 15) {
			return null;
		}
		for (int i = 0; i < len; i++) {
			char c = src.charAt(i);
			if (c == '.') {
				if (newOctet || tmpValue < 0 || tmpValue > 0xff || currByte == 3) {
					return null;
				}
				res[currByte++] = (byte) (tmpValue & 0xff);
				tmpValue = 0;
				newOctet = true;
			} else {
				int digit = c - '0';

				if (digit < 0 || digit >= 10) {
					return null;
				}

				tmpValue *= 10;
				tmpValue += digit;
				newOctet = false;
			}
		}
		if (newOctet || tmpValue < 0 || tmpValue >= (1L << ((4 - currByte) * 8))) {
			return null;
		}
		switch (currByte) {
			case 0:
				res[0] = (byte) ((tmpValue >> 24) & 0xff);
			case 1:
				res[1] = (byte) ((tmpValue >> 16) & 0xff);
			case 2:
				res[2] = (byte) ((tmpValue >>  8) & 0xff);
			case 3:
				res[3] = (byte) (tmpValue & 0xff);
		}
		return res;
	}
}
