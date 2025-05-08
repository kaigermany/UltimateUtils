package me.kaigermany.ultimateutils.networking.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SocketFactory {
	private static final javax.net.SocketFactory secureSSLFactory = SSLSocketFactory.getDefault();
	private static final javax.net.SocketFactory insecureSSLFactory = getSocketFactory();
	private static Proxy proxy;

	/**
	 * Creates a new Socket. <br />
	 * if ssl then an new SSLSocket will be opened. <br />
	 * if ssl && disableCertificateCheck then an new SSLSocket will be opened
	 * but the certificate will not be checked. This can become handy if you are
	 * working with self-signed or out-dated certificates.
	 * 
	 * @param ip IPv4, PIv6 or DNS address to connect to
	 * @param port the target port to open
	 * @param ssl use an SSLSocket or else a plain (raw) Socket
	 * @param disableCertificateCheck ignores invalid or unknown certificates. works only if boolean ssl is also true.
	 * @return a new Socket ready to communicate.
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public static Socket openConnection(String ip, int port, boolean ssl, boolean disableCertificateCheck)
		throws UnknownHostException, IOException {
		Socket socket;

		if (proxy != null) {
			if (ssl) {
				socket = new Socket(proxy);
				socket.connect(new InetSocketAddress(ip, port));

				javax.net.SocketFactory factory = disableCertificateCheck ? insecureSSLFactory : secureSSLFactory;

				if (factory instanceof javax.net.ssl.SSLSocketFactory) {
					return ((javax.net.ssl.SSLSocketFactory) factory).createSocket(
						socket, ip, port, true /* autoClose */);
				} else {
					throw new IOException("SSL über SOCKS-Proxy benötigt eine SSLSocketFactory");
				}
			} else {
				socket = new Socket(proxy);
				socket.connect(new InetSocketAddress(ip, port));
				return socket;
			}
		} else {
			if (ssl) {
				return (disableCertificateCheck ? insecureSSLFactory : secureSSLFactory).createSocket(ip, port);
			} else {
				return new Socket(ip, port);
			}
		}
	}

	/**
	 * Creates a new SSLSocketFactory that ignores all certificate validations
	 * based on: <a href=
	 * "https://stackoverflow.com/questions/12060250/ignore-ssl-certificate-errors-with-java">
	 * ...</a>
	 *
	 * @return SSLSocketFactory
	 */
	private static SSLSocketFactory getSocketFactory() {
		TrustManager[] certs = new TrustManager[] { new X509TrustManager() {
			@Override public X509Certificate[] getAcceptedIssuers() { return null; }
			@Override public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
			@Override public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
		}};
	    
	    try {
	    	SSLContext ctx = SSLContext.getInstance("SSL");
	        ctx.init(null, certs, new SecureRandom());
		    return ctx.getSocketFactory();
	    } catch (java.security.GeneralSecurityException ex) {
	    	ex.printStackTrace();
	    	return null;
	    }
	}

	public static void setProxy(Proxy proxy) {
		SocketFactory.proxy = proxy;
	}
}
