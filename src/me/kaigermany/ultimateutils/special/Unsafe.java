package me.kaigermany.ultimateutils.special;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Unsafe {
	public static final /* Unsafe */Object unsafe;
	public static final Class<?> unsafeClass;
	
	public static final Method allocateInstancePointer;
	
	static {
		{
			Object unsafeTemp = null;
			Class<?> unsafeClassTemp = null;
			try {
				unsafeClassTemp = Class.forName("sun.misc.Unsafe");
				Field f = unsafeClassTemp.getDeclaredField("theUnsafe");
				f.setAccessible(true);
				unsafeTemp = f.get(null);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			unsafe = unsafeTemp;
			unsafeClass = unsafeClassTemp;
		}
		{
			Method allocateInstancePointerTemp = null;
			try {
				allocateInstancePointerTemp = unsafeClass.getMethod("allocateInstance", Class.class);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			allocateInstancePointer = allocateInstancePointerTemp;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T allocateNewInstance(Class<T> clazz) throws Exception {
		return (T)allocateInstancePointer.invoke(unsafe, clazz);
	}
}
