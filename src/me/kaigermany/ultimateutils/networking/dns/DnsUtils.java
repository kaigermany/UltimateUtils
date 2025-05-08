package me.kaigermany.ultimateutils.networking.dns;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public class DnsUtils {
	//based on https://stackoverflow.com/questions/41265266/how-to-solve-inaccessibleobjectexception-unable-to-make-member-accessible-m
	public static final Class<?> unsafeClass;
	public static final Object unsafe;
		
	static{
		Object unsafeInstance = null;
		Class<?> unsafeClassType = null;
		try{
			unsafeClassType = Class.forName("sun.misc.Unsafe");
			Field f = unsafeClassType.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			unsafeInstance = f.get(null);
		}catch(Exception e){
			e.printStackTrace();
		}
		unsafe = unsafeInstance;
		unsafeClass = unsafeClassType;
	}
//TODO test that updates are compatible
	public static void unsafeSetField(Class<?> targetClass, Field targtFieldInClass, Object objectToSet) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		//TODO automatically switch between static an instantiated calls.
		//boolean isStatic = (targtFieldInClass.getModifiers() & Modifier.STATIC) != 0;
		Object long_offset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class).invoke(unsafe, targtFieldInClass);
		unsafeClass.getDeclaredMethod("putObject", Object.class, long.class, Object.class).invoke(unsafe, targetClass, long_offset, objectToSet);
	}
	
	public static Object unsafeGetField(Class<?> targetClass, Field targtFieldInClass) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Object long_offset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class).invoke(unsafe, targtFieldInClass);
		return unsafeClass.getDeclaredMethod("getObject", Object.class, long.class).invoke(unsafe, targetClass, long_offset);
	}
	
	public static void unsafeSetDynamicField(Field targtFieldInClass, Object instance, Object objectToSet) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Object long_offset = unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class).invoke(unsafe, targtFieldInClass);
		unsafeClass.getDeclaredMethod("putObject", Object.class, long.class, Object.class).invoke(unsafe, instance, long_offset, objectToSet);
	}
	
	public static Object unsafeGetDynamicField(Field targtFieldInClass, Object instance) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Object long_offset = unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class).invoke(unsafe, targtFieldInClass);
		return unsafeClass.getDeclaredMethod("getObject", Object.class, long.class).invoke(unsafe, instance, long_offset);
	}
}
