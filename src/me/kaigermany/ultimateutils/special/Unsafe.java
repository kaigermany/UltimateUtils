package me.kaigermany.ultimateutils.special;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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

	public static Object getStaticField(Class<?> targetClass, Field targetFieldInClass) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Long off = (Long)unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class).invoke(unsafe, targetFieldInClass);
		return unsafeClass.getDeclaredMethod("getObject", Object.class, long.class).invoke(unsafe, targetClass, off);
	}
	
	public static void setStaticField(Class<?> targetClass, Field targetFieldInClass, Object objectToSet) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Long off = (Long)unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class).invoke(unsafe, targetFieldInClass);
		unsafeClass.getDeclaredMethod("putObject", Object.class, long.class, Object.class).invoke(unsafe, targetClass, off, objectToSet);
	}

	public static Object getDynamicField(Object instance, Field targetFieldInClass) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Long off = (Long)unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class).invoke(unsafe, targetFieldInClass);
		return unsafeClass.getDeclaredMethod("getObject", Object.class, long.class).invoke(unsafe, instance, off);
	}
	
	public static void setDynamicField(Object instance, Field targetFieldInClass, Object objectToSet) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Long off = (Long)unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class).invoke(unsafe, targetFieldInClass);
		unsafeClass.getDeclaredMethod("putObject", Object.class, long.class, Object.class).invoke(unsafe, instance, off, objectToSet);
	}
}
