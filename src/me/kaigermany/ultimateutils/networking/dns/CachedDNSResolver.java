package me.kaigermany.ultimateutils.networking.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class CachedDNSResolver implements DnsResolver {
	private Map<String, InetAddress> knownAddresses = new HashMap<String, InetAddress>();
	private DnsResolver resolver;
	
	public CachedDNSResolver(DnsResolver resolver) {
		this.resolver = resolver;
	}
	
	@Override
	public InetAddress resolve(String hostname) throws UnknownHostException {
		InetAddress addr;
		synchronized (knownAddresses) {
			addr = knownAddresses.get(hostname);
		}
		if(addr == null){
			addr = resolver.resolve(hostname);
			synchronized (knownAddresses) {
				knownAddresses.put(hostname, addr);
			}
		}
		return addr;
	}
}
