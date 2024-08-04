package me.kaigermany.ultimateutils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/*
 * Allocate and free Direct ByteBuffers.
 */
public class DirectMemory {
	private static Object unsafe;
	private static Method invokeCleanerMethod;
	private static Method oldCleanerMethod;

	static {
		int version;
		{
			String versionStr = System.getProperty("java.version");
			if (versionStr.startsWith("1.")) {
				versionStr = versionStr.substring(2, 3);
			} else {
				int dot = versionStr.indexOf(".");
				if (dot != -1) {
					versionStr = versionStr.substring(0, dot);
				}
			}
			version = Integer.parseInt(versionStr);
		}
		System.out.println("version=" + version);
		if (version < 8) {
			System.err.println("DirectMemory: WARNING: your version seems to be too old (" + version
					+ "). So it cant savely provide you a cleanup function!");
		} else {
			try {
				Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
				Field f = unsafeClass.getDeclaredField("theUnsafe");
				f.setAccessible(true);
				unsafe = f.get(null);

				if (version >= 9) {
					invokeCleanerMethod = unsafeClass.getDeclaredMethod("invokeCleaner", ByteBuffer.class);
				} else {
					oldCleanerMethod = Class.forName("sun.nio.ch.DirectBuffer").getMethod("cleaner");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private DirectMemory() {}
	
	/*
	 * Allocates a Buffer by a given size.
	 */
	public static ByteBuffer alloc(int bytes) {
		return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
	}
	/*
	 * deallocates a given ByteBuffer.
	 * Useful if you want to free up this space as fast as possible.
	 */
	public static void free(ByteBuffer bb) {
		if (!bb.isDirect()) {
			return;
		}
		// j8: ((sun.nio.ch.DirectBuffer)bb).cleaner().clean();
		// j9+: unsafe.invokeCleaner(bb);
		try {
			if (invokeCleanerMethod != null) {
				invokeCleanerMethod.invoke(unsafe, bb);
			} else if (oldCleanerMethod != null) {
				Object cleanerObj = oldCleanerMethod.invoke(bb);
				cleanerObj.getClass().getDeclaredMethod("clean").invoke(cleanerObj);
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
}
