package me.kaigermany.ultimateutils.networking.httpserver;

public class MimeTypeConverter {

	public static String getMIMEtype(String f) {
		switch (f.substring(f.lastIndexOf('.') + 1)) {
			// Image
			case "jpg":
				return "image/jpeg";
			case "jpeg":
				return "image/jpeg";
			case "jpe":
				return "image/jpeg";
			case "png":
				return "image/png";
			case "gif":
				return "image/gif";
			case "svg":
				return "image/svg+xml";
			case "ico":
				return "image/x-icon";
			case "bmp":
				return "image/bmp";
	
			// Audio
			case "mp3":
				return "audio/mpeg";
			case "ogg":
				return "audio/ogg";
			case "ogv":
				return "audio/ogg";
	
			// Video
			case "mp4":
				return "video/mp4";
			// case "ogg": return "video/ogg";
			case "mkv":
				return "video/x-matroska";
	
			// Text
			case "css":
				return "text/css";
			case "html":
				return "text/html";
			case "htm":
				return "text/html";
			case "shtml":
				return "text/html";
			case "txt":
				return "text/plain";
	
			// application
			case "pdf":
				return "application/pdf";
			case "js":
				return "application/javascript";
			case "json":
				return "application/json";
			case "xml":
				return "application/xml";
				
			default:
				return "text/html";
		}
	}
}
