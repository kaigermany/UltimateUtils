package me.kaigermany.ultimateutils.networking.dns;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class DnsUtils {
	//based on https://stackoverflow.com/questions/41265266/how-to-solve-inaccessibleobjectexception-unable-to-make-member-accessible-m
		public static /* Unsafe */Object unsafe;
		public static Class<?> unsafeClass;

		public static void unsafeSetField(Class<?> targetClass, Field targtFieldInClass, Object objectToSet) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, NoSuchFieldException {
			unsafeClass = Class.forName("sun.misc.Unsafe");
			Field f = unsafeClass.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			unsafe = f.get(null);
			
			Long off = (Long)unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class).invoke(unsafe, targtFieldInClass);
			unsafeClass.getDeclaredMethod("putObject", Object.class, long.class, Object.class).invoke(unsafe, targetClass, off, objectToSet);
		}
		
		public static Object unsafeGetField(Class<?> targetClass, Field targtFieldInClass) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, NoSuchFieldException {
			unsafeClass = Class.forName("sun.misc.Unsafe");
			Field f = unsafeClass.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			unsafe = f.get(null);
			
			Long off = (Long)unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class).invoke(unsafe, targtFieldInClass);
			return unsafeClass.getDeclaredMethod("getObject", Object.class, long.class).invoke(unsafe, targetClass, off);
		}
		public static void unsafeSetDynamicField(Field targtFieldInClass, Object instance, Object objectToSet) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, NoSuchFieldException {
			unsafeClass = Class.forName("sun.misc.Unsafe");
			Field f = unsafeClass.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			unsafe = f.get(null);
			
			Long off = (Long)unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class).invoke(unsafe, targtFieldInClass);
			unsafeClass.getDeclaredMethod("putObject", Object.class, long.class, Object.class).invoke(unsafe, objectToSet, off, objectToSet);
		}
}
