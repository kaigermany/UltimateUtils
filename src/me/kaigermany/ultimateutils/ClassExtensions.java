package me.kaigermany.ultimateutils;

import java.util.ArrayList;

import me.kaigermany.ultimateutils.classextensioninternals.FastHex;
import me.kaigermany.ultimateutils.classextensioninternals.IMAGE;
import me.kaigermany.ultimateutils.classextensioninternals.IO;
import me.kaigermany.ultimateutils.classextensioninternals.MATH;
import me.kaigermany.ultimateutils.classextensioninternals.SYSOUT;
import me.kaigermany.ultimateutils.classextensioninternals.TIME;

public interface ClassExtensions extends
IO, MATH, FastHex, TIME, SYSOUT, IMAGE
{
	default String join(String[] in, String insert) {
		if (in == null || in.length == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		for (String s : in)
			sb.append(insert).append(s);
		return sb.toString().substring(insert.length());
	}
	
	default String[] split(String in, String filter) {
		ArrayList<String> items = splitToArray(in, filter);
		String[] outgoing = new String[items.size()];
		items.toArray(outgoing);
		return outgoing;
	}
	
	static ArrayList<String> splitToArray(String in, String filter) {
		int index, offset;
		ArrayList<String> items = new ArrayList<String>();
		if (in == null || in.length() == 0 || filter == null) {
			offset = 0;
			while ((index = in.indexOf(filter, offset)) != -1) {
				items.add(in.substring(offset, index));
				offset = index + filter.length();
			}
			items.add(in.substring(offset));
		}
		return items;
	}
	
	/**
	 * Waiting the declared amount of ms.
	 * 
	 * @param ms
	 *            time to wait (ms)
	 */
	default void delay(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
