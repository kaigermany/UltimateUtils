package me.kaigermany.ultimateutils.special;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

public class UnknownResourceInjector8 {
	public static void injectForJava8() throws MalformedURLException, NoSuchMethodException, SecurityException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchFieldException{
		ClassLoader system = ClassLoader.getSystemClassLoader();
		Field parentClassLoader = null;
		try {
			parentClassLoader = ClassLoader.class.getDeclaredField("parent");
		} catch (Exception ignored) {//maybe renamed
			for(Field f : ClassLoader.class.getDeclaredFields()){
				if(f.getType() == ClassLoader.class){
					parentClassLoader = f;
				}
			}
		}
		parentClassLoader.setAccessible(true);
		parentClassLoader.set(system, new ClassLoaderInjectionSystem((ClassLoader)parentClassLoader.get(system)));
	}

	public static class ClassLoaderInjectionSystem extends ClassLoader {
		public ClassLoaderInjectionSystem(ClassLoader classLoader){
			super(classLoader);
		}
		
		@Override
		public URL getResource(String name) {
			try {
				return UnknownResourceListener.createURL(name);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		@Override
		public InputStream getResourceAsStream(String name) {
			return UnknownResourceListener.getResource(name);
		}
	}
}
