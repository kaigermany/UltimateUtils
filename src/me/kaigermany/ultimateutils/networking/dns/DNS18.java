package me.kaigermany.ultimateutils.networking.dns;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.util.ArrayList;

public class DNS18 {
	//based on https://stackoverflow.com/questions/41265266/how-to-solve-inaccessibleobjectexception-unable-to-make-member-accessible-m
	public static void inject(final DnsResolver resolver) throws ClassNotFoundException, NoSuchFieldException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
		Object proxyObject = Proxy.newProxyInstance(DNSRequestRedirector.class.getClassLoader(), new Class[] {Class.forName("java.net.spi.InetAddressResolver")}, new InvocationHandler() {
			//java.util.stream.Stream java.net.spi.InetAddressResolver.lookupByName(java.lang.String,java.net.spi.InetAddressResolver$LookupPolicy) throws java.net.UnknownHostException
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if(!method.getName().equals("lookupByName")) {
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
				
				ArrayList<InetAddress> tmp = new ArrayList<InetAddress>(1);
				tmp.add(resolver.resolve((String)args[0]));
				return (java.util.stream.Stream<InetAddress>)tmp.stream();
			}
		});
		//Class<?> clazz = Class.forName("java.lang.ApplicationShutdownHooks");
		//Field field = clazz.getDeclaredField("hooks");
		//Object m = safe.getObject(clazz, safe.staticFieldOffset(field));
		DnsUtils.unsafeSetField(InetAddress.class, InetAddress.class.getDeclaredField("resolver"), proxyObject);
	}
}
