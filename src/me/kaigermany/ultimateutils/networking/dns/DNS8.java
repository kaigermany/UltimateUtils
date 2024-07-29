package me.kaigermany.ultimateutils.networking.dns;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;

public class DNS8 {
	@SuppressWarnings("unchecked")
	public static void inject(final DnsResolver resolver) throws NoSuchFieldException, SecurityException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException {
		Object proxyObject = Proxy.newProxyInstance(DNSRequestRedirector.class.getClassLoader(), new Class[] {Class.forName("sun.net.spi.nameservice.NameService")}, new InvocationHandler() {
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
		
		Field f = InetAddress.class.getDeclaredField("nameServices");
		f.setAccessible(true);
		java.util.List<Object> nameServices = (java.util.List<Object>)f.get(null);
		nameServices.clear();
		nameServices.add(proxyObject);
	}
}
