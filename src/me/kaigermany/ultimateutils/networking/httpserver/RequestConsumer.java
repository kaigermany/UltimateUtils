package me.kaigermany.ultimateutils.networking.httpserver;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface RequestConsumer {
	boolean accept(String requestMethod, String requestPath, String requestProtocolVersion,
			Map<String, List<String>> requestHeaders, InputStream is, OutputStream os);
}
