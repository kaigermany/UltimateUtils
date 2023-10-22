package me.kaigermany.ultimateutils.networking.util;

import java.io.IOException;
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
	public static Socket openConnection(String ip, int port, boolean ssl, boolean disableCertificateCheck) throws UnknownHostException, IOException {
		if(ssl){
			return (disableCertificateCheck ? insecureSSLFactory : secureSSLFactory).createSocket(ip, port);
		} else {
			return new Socket(ip, port);
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
}
