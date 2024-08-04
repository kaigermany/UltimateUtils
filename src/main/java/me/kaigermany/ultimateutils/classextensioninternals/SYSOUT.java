package me.kaigermany.ultimateutils.classextensioninternals;

import java.util.Arrays;

public interface SYSOUT extends FastHex {
	/**
	 * Prints a Object[] into System.out.
	 * @param arr data to print
	 */
	default void printArray(Object[] arr) {
		System.out.println(Arrays.deepToString(arr));
	}

	/**
	 * Prints a String[] into System.out.
	 * @param arr data to print
	 */
	default void printArray(String[] arr) {
		System.out.println(Arrays.deepToString(arr));
	}

	/**
	 * Prints the given generic array into System.out.
	 * @param arr data to print
	 */
	default void printArray(boolean[] arr) {
		String[] a = new String[arr.length];
		for (int i = 0; i < arr.length; i++) {
			a[i] = String.valueOf(arr[i]);
		}
		printArray(a);
	}

	/**
	 * Prints the given generic array into System.out.
	 * @param arr data to print
	 */
	default void printArray(byte[] arr) {
		String[] a = new String[arr.length];
		for (int i = 0; i < arr.length; i++) {
			a[i] = String.valueOf(arr[i]);
		}
		printArray(a);
	}

	/**
	 * Prints the given generic array into System.out.
	 * @param arr data to print
	 */
	default void printArray(short[] arr) {
		String[] a = new String[arr.length];
		for (int i = 0; i < arr.length; i++) {
			a[i] = String.valueOf(arr[i]);
		}
		printArray(a);
	}

	/**
	 * Prints the given generic array into System.out.
	 * @param arr data to print
	 */
	default void printArray(char[] arr) {
		String[] a = new String[arr.length];
		for (int i = 0; i < arr.length; i++) {
			a[i] = String.valueOf(arr[i]);
		}
		printArray(a);
	}

	/**
	 * Prints the given generic array into System.out.
	 * @param arr data to print
	 */
	default void printArray(int[] arr) {
		String[] a = new String[arr.length];
		for (int i = 0; i < arr.length; i++) {
			a[i] = String.valueOf(arr[i]);
		}
		printArray(a);
	}

	/**
	 * Prints the given generic array into System.out.
	 * @param arr data to print
	 */
	default void printArray(long[] arr) {
		String[] a = new String[arr.length];
		for (int i = 0; i < arr.length; i++) {
			a[i] = String.valueOf(arr[i]);
		}
		printArray(a);
	}

	/**
	 * Prints the given generic array into System.out.
	 * @param arr data to print
	 */
	default void printArray(float[] arr) {
		String[] a = new String[arr.length];
		for (int i = 0; i < arr.length; i++) {
			a[i] = String.valueOf(arr[i]);
		}
		printArray(a);
	}

	/**
	 * Prints the given generic array into System.out.
	 * @param arr data to print
	 */
	default void printArray(double[] arr) {
		String[] a = new String[arr.length];
		for (int i = 0; i < arr.length; i++) {
			a[i] = String.valueOf(arr[i]);
		}
		printArray(a);
	}

	/**
	 * Prints the given byte[] into System.out.
	 * The formatting is the classical 16-bytes-per-row hex dump style,
	 * same bytes as ascii chars will be printed on the right side, the hex offset of the current row on the left.
	 * @param arr data to print
	 */
	default void binaryDump(byte[] in) {
		int offset = 0;
		while (true) {
			System.out.print(hexInt(offset) + " ");
			for (int i = 0; i < 16; i++) {
				int a = offset + i;
				if (a < in.length)
					System.out.print(hexByte(in[a]) + " ");
				else
					System.out.print("   ");
			}
			for (int i = 0; i < 16; i++) {
				int a = offset + i;
				if (a < in.length)
					System.out.print((char) in[a]);
				else
					System.out.print(" ");
			}
			System.out.println();
			offset += 16;
			if (offset >= in.length)
				break;
		}
	}
}
