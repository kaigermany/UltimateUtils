package me.kaigermany.ultimateutils.networking.smarthttp;

import java.io.InputStream;

public interface HTTPResultEvent {
	void onReceived(HTTPResult result, InputStream inputStream);
}
