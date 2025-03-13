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
	/**
	 * Merges a given String[] and add a given String in between.
	 * @param in Arguments to merge
	 * @param insert The word that will be added in between each argument
	 * @return the combined data
	 */
	default String join(String[] in, String insert) {
		if (in == null || in.length == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		for (String s : in)
			sb.append(insert).append(s);
		return sb.toString().substring(insert.length());
	}
	
	/**
	 * A simplified version of String.split() - it has NO REGEX! If you want to use REGEX, consider using String.split() instead!
	 * This split function just searches for the exact occurrence of the phrase String filter and nothing else.
 	 * @param in argument to split
	 * @param filter word to search for
	 * @return a list of all arguments before and after each hit of the word search.
	 */
	default String[] split(String in, String filter) {
		ArrayList<String> items = splitToArray(in, filter);
		return items.toArray(new String[items.size()]);
	}
	
	/**
	 * A simplified version of String.split() - it has NO REGEX! If you want to use REGEX, consider using String.split() instead!
	 * This split function just searches for the exact occurrence of the phrase String filter and nothing else.
 	 * @param in argument to split
	 * @param filter word to search for
	 * @return a list of all arguments before and after each hit of the word search.
	 */
	default ArrayList<String> splitToArray(String in, String filter) {
		int index, offset;
		ArrayList<String> items = new ArrayList<String>();
		if (in != null && in.length() != 0 && filter != null) {
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
