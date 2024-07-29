package me.kaigermany.ultimateutils.networking.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;

public interface DnsResolver {
	InetAddress resolve(String hostname) throws UnknownHostException;
}
