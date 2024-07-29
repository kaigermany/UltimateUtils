package me.kaigermany.ultimateutils.networking.dns;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;

public class DNS9 {
	//TODO https://github.com/AdoptOpenJDK/openjdk-jdk11/blob/master/src/java.base/share/classes/java/net/InetAddress.java#L891
	public static void inject(final DnsResolver resolver) throws NoSuchFieldException, SecurityException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Class<?> proxyInterface = Class.forName("java.net.InetAddressImpl");
		Object proxyObject = Proxy.newProxyInstance(/*DNS_TEST.class*/proxyInterface.getClassLoader(), new Class[] {proxyInterface}, new InvocationHandler() {
			//InetAddress[] lookupAllHostAddr(String paramString) throws UnknownHostException {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if(!method.getName().equals("lookupAllHostAddr")) {
					return null;
				}
				/*
				//System.out.println("invoke: proxy=" + proxy + ", method="+method + ", args=" + Arrays.deepToString(args));
				System.out.println("invoke: proxy:" + proxy.getClass());
				System.out.println("        method:" + method);
				System.out.println("        args:" + args.length);
				for(int i=0; i<args.length; i++) {
					System.out.println("        ["+i+"] : " + args[i]);
				}
				*/
				
				return new InetAddress[] { resolver.resolve((String)args[0]) };
			}
		});
		
		DnsUtils.unsafeSetField(InetAddress.class, InetAddress.class.getDeclaredField("impl"), proxyObject);
	}
}
