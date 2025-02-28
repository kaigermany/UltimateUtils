package me.kaigermany.ultimateutils.special;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Objects;

/*
 * once injected, it can react on resource request that can't satisfied by the system until now.
 * 
 * example use case is an dirty-written resource search that can't be answered by the ClassLoader of the class that contains this call:
 * "".getClass().getResourceAsStream("abc.txt");
 * by design, the call will be handled by String's ClassLoader, and not your own ClassLoader that loaded this code, so the request will fail.
 * 
 * Note: Java17 does NOT work reliable right now!
 * Successfully tested with Java 8, Java 11, Java 21.
 */
public class UnknownResourceListener {
	public static void main(String[] args){//TEST
		try{
			addListener(new RessourceRequest() {
				@Override
				public InputStream tryFindRessource(String name, Thread requestingThread) {
					System.out.println("request = " + name);
					if(name.equals("abc.def")) return new ByteArrayInputStream(new byte[]{12});
					if(name.equals("abc/def")) return new ByteArrayInputStream(new byte[]{98});
					return null;
				}
			});

			inject();

			System.gc();
			System.out.println("try getResourceAsStream()...");
			//ModuleDescriptor md = "".getClass().getModule().getDescriptor();

			//System.out.println("md.isOpen(): " + md.isOpen());

			//Objects.requireNonNull("".getClass().getResourceAsStream("abc.def"));
			//Objects.requireNonNull("".getClass().getResourceAsStream("abc/def"));
			Objects.requireNonNull("".getClass().getResourceAsStream("/abc/def"));
			if("".getClass().getResourceAsStream("de/abc/def") != null){
				System.err.println("expected null, but got instance!");
			}

			System.out.println("SUCCESS!");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static interface RessourceRequest {
		InputStream tryFindRessource(String name, Thread requestingThread);
	}
	
	public static class InjectionException extends Exception {
		private static final long serialVersionUID = 3817437336301156916L;
		public InjectionException(String msg){
			super(msg);
		}
	}

	private static ArrayList<RessourceRequest> eventListeners = new ArrayList<RessourceRequest>();

	public static void addListener(RessourceRequest eventHandler) {
		synchronized (eventListeners) {
			eventListeners.add(eventHandler);
		}
	}

	public static void removeListener(RessourceRequest eventHandler) {
		synchronized (eventListeners) {
			eventListeners.remove(eventHandler);
		}
	}

	public static void inject() throws Exception {
		try {
			if(getJavaVersion() >= 9){
				throw new Exception("injectForJava8() is not compatible with Java version " + getJavaVersion());
			}
			UnknownResourceInjector8.injectForJava8();
		} catch(Throwable e8) {
			try {
				UnknownResourceInjector9.injectForJava9();
				awaitLazyCache();
			} catch(Throwable e9) {
				InjectionException ex = new InjectionException("unable to inject for Java 8 or Java 9+");
				ex.initCause(e9);
				e9.initCause(e8);
				throw ex;
			}
		}
		System.out.println("inject done.");
	}
	
	private static int getJavaVersion(){
		String v = System.getProperty("java.runtime.version");
		if(v.startsWith("1.")) {//remove left text part
			v = v.substring(2);
		}
		int p = v.indexOf('.');//remove right text part
		if(p != -1){
			v = v.substring(0, p);
		}
		return Integer.parseInt(v);
	}
	
	private static void awaitLazyCache() throws Exception {
		final String keyword = "some/unknown/source.file";
		RessourceRequest unitTestRequestHook = new RessourceRequest() {
			@Override
			public InputStream tryFindRessource(String name, Thread requestingThread) {
				System.out.println("awaitLazyCache(): request = " + name);
				if(name.equals(keyword)) return new ByteArrayInputStream(new byte[0]);
				return null;
			}
		};
		addListener(unitTestRequestHook);
		boolean success = false;
		for(int i=0; i<30; i++){
			if("".getClass().getResourceAsStream(keyword) != null){
				success = true;
				break;
			}
			Thread.sleep(100);
			//System.gc();
		}
		/*
		for(int i=0; i<30; i++){
			if(UnknownResourceListener.class.getResourceAsStream(keyword) != null){
				success = true;
				break;
			}
			Thread.sleep(100);
			//System.gc();
		}
		*/
		removeListener(unitTestRequestHook);
		if(!success) throw new Exception("awaitLazyCache() was not successful!");
	}

	public static InputStream getResource(String name) {
		Thread thread = Thread.currentThread();
		InputStream is = null;
		synchronized (eventListeners) {
			System.out.println("getResource(): eventListeners.size() = " + eventListeners.size());
			for (RessourceRequest rr : eventListeners) {
				if ((is = rr.tryFindRessource(name, thread)) != null) break;
			}
		}
		System.out.println("FOUND? " + (is != null));
		return is;
	}

	public static URL createURL(String name) throws MalformedURLException {
		return createURL(getResource(name));
	}

	public static URL createURL(final InputStream is) throws MalformedURLException {
		return new URL("", "", 0, "", new URLStreamHandler(){
			@Override
			protected URLConnection openConnection(URL u) throws IOException {
				return new URLConnection(u) {
					@Override
					public void connect() throws IOException {}
					public InputStream getInputStream() throws IOException {
						System.out.println("getInputStream() #2");
						return is;
					};
				};
			}
		});
	}
	
	


}
