package me.kaigermany.ultimateutils.networking.dns;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

public abstract class DnsResolver {
	public abstract InetAddress resolve(String hostname) throws UnknownHostException;
	
	/**
	 * Executes a simple HTTP-GET call to allow simple DNS-OVER-HTTPS operations..
	 * note: it only wants to accept 'application/dns-json'-contents.
	 * 
	 * @param address
	 *            a normal fully qualified url.
	 * @return the bytes received from the server.
	 * @throws IOException
	 */
	
	protected static byte[] serverGet(String address) throws IOException {
		URL url = new URL(address);
		URLConnection conn = (URLConnection) url.openConnection();
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:75.0) Gecko/20100101 Firefox/75.0");
		conn.setRequestProperty("Accept", "application/dns-json");

		conn.setRequestProperty("Accept-Language", "de,en-US;q=0.7,en;q=0.3");
		BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int chr;
		byte[] buffer = new byte[1024];
		while ((chr = bis.read(buffer)) != -1) {
			baos.write(buffer, 0, chr);
		}
		bis.close();
		return baos.toByteArray();
	}
	
	/**
	 * Parses the IPv4 digits out of a given String.
	 * In case the String format is incompatible (expects at least 'x.y.z.w', at most 'xxx.yyy.zzz.www') null is returned.
	 * the returned array has the exact same order: [x, y, z, w].
	 * @param src
	 *            the IPv4 text representation
	 * @return four bytes of IPv4.
	 */
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
