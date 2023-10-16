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
	
	public static Socket openConnection(String ip, int port, boolean ssl, boolean disableCertificateCheck) throws UnknownHostException, IOException {
		if(ssl){
			return (disableCertificateCheck ? insecureSSLFactory : secureSSLFactory).createSocket(ip, port);
		} else {
			return new Socket(ip, port);
		}
	}
	
	//based on https://stackoverflow.com/questions/12060250/ignore-ssl-certificate-errors-with-java
	private static SSLSocketFactory getSocketFactory() {
		TrustManager[] certs = new TrustManager[] { new X509TrustManager() {
			@Override public X509Certificate[] getAcceptedIssuers() { return null; }
			@Override public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
			@Override public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
		}};
	    
	    SSLContext ctx = null;
	    try {
	        ctx = SSLContext.getInstance("SSL");
	        ctx.init(null, certs, new SecureRandom());
	    } catch (java.security.GeneralSecurityException ex) {
	    	ex.printStackTrace();
	    	return null;
	    }
	    return ctx.getSocketFactory();
	}
}
