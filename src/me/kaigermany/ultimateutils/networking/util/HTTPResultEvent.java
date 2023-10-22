package me.kaigermany.ultimateutils.networking.util;

import java.io.InputStream;

public interface HTTPResultEvent {
	void onReceived(HTTPResult result, InputStream inputStream);
}
