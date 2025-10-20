package me.kaigermany.ultimateutils.networking.httpserver;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public abstract class FilteringRequestConsumer implements RequestConsumer {
	@Override
	public boolean accept(String requestMethod, String requestPath, String requestProtocolVersion,
			Map<String, List<String>> requestHeaders, InputStream is, OutputStream os) {
		if(!patternMatchPath(requestPath)){
			return false;
		}
		handle(requestMethod, requestPath, requestHeaders, is, os);
		return true;
	}
	
	private final boolean patternMatchPath(String filter){
		final String pattern = getPathFilter();
		boolean prefix = pattern.charAt(0) == '*';
		if(prefix && pattern.length() == 1) {
			return true;
		}
		boolean postfix = pattern.charAt(pattern.length() - 1) == '*';
		if(prefix){
			if(postfix){
				return filter.contains(pattern.substring(1, pattern.length() - 1));
			} else {
				return filter.endsWith(pattern.substring(1));
			}
		} else {
			if(postfix){
				return filter.startsWith(pattern.substring(0, pattern.length() - 1));
			} else {
				return filter.equals(pattern);
			}
		}
	}
	
	public abstract String getPathFilter();

	public abstract void handle(String requestMethod, String requestPath,
			Map<String, List<String>> requestHeaders, InputStream is, OutputStream os);
}
