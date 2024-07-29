package me.kaigermany.ultimateutils.networking.dns;

public class DNSRequestRedirector {
	private static DnsResolver externalResolver;
	
	public static boolean injectResolver(DnsResolver resolver) {
		if(resolver == null) return false;
		
		boolean notInjected = externalResolver == null;
		externalResolver = resolver;
		
		if(notInjected) {
			Exception j8, j9, j18;
			try {
				DNS8.inject(externalResolver);
				return true;
			}catch (Exception e) {
				j8 = e;
			}
			try {
				DNS9.inject(externalResolver);
				return true;
			}catch (Exception e) {
				j9 = e;
			}
			try {
				DNS18.inject(externalResolver);
				return true;
			}catch (Exception e) {
				j18 = e;
			}
			System.err.println("unable to inject DNS resolver:");
			System.err.println("unable to inject for Java8:");
			j8.printStackTrace();
			System.err.println("unable to inject for Java9+:");
			j9.printStackTrace();
			System.err.println("unable to inject for Java18+:");
			j18.printStackTrace();
			return false;
		}
		
		return true;
	}
}
