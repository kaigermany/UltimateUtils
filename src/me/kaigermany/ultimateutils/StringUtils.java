package me.kaigermany.ultimateutils;

import java.util.ArrayList;

public class StringUtils {
	/**
	 * Simple String cutting method.
	 * By defining a left-side and right-side filter, the inner part gets identified and will be returned.
	 * 
	 * @param in
	 *            Data-Source
	 * @param filterLeft
	 *            String has to start right behind this text (NO REGEX)
	 * @param filterRight
	 *            String has to end just before this text (NO REGEX)
	 * @return the text in between both left and right filters.
	 */
	public static String splitAndKeepMiddle(String in, String filterLeft, String filterRight) {
		int pL = in.indexOf(filterLeft);
		if (pL == -1) {
			throw new IllegalArgumentException("Left-side filter missmatch");
		}
		pL += filterLeft.length();
		int pR = in.indexOf(filterRight, pL);
		if (pR == -1) {
			throw new IllegalArgumentException("Right-side filter missmatch");
		}
		return in.substring(pL, pR);
	}
	
	/**
	 * Simple String cutting method.
	 * By defining a left-side and right-side filter, the inner part gets identified and will be returned.
	 * In case that the left filter is missing, the left side will start at offset 0.
	 * In case that the right filter is missing, the right side will reach to the end of the input String.
	 * If both filters fail, the resulting string effectively equals the input String.
	 * 
	 * @param in
	 *            Data-Source
	 * @param filterLeft
	 *            String has to start right behind this text (NO REGEX)
	 * @param filterRight
	 *            String has to end just before this text (NO REGEX)
	 * @return the text in between both left and right filters.
	 */
	public static String splitAndKeepMiddleUnsave(String in, String filterLeft, String filterRight) {
		int pL = in.indexOf(filterLeft);
		if (pL == -1) {
			pL = 0;
		} else {
			pL += filterLeft.length();
		}
		int pR = in.indexOf(filterRight, pL);

		if (pR == -1) pR = in.length();
		return in.substring(pL, pR);
	}
	
	/**
	 * Simple Split-method by explicit String.
	 * 
	 * @param in
	 *            Data-Source
	 * @param filter
	 *            String where to Split (NO REGEX)
	 * @return result-list (ArrayList)
	 */
	public static ArrayList<String> splitToArray(String in, String filter) {
		int index, offset;
		ArrayList<String> items = new ArrayList<String>();
		if (in == null || in.length() == 0 || filter == null) return items;
		offset = 0;
		while ((index = in.indexOf(filter, offset)) != -1) {
			items.add(in.substring(offset, index));
			offset = index + filter.length();
		}
		items.add(in.substring(offset));
		return items;
	}
	


	public static ArrayList<String> splitStringSave(String in, String filter) {
		ArrayList<String> result = new ArrayList<String>();
		StringBuilder currentPart = new StringBuilder();
		boolean insideQuotes = false;
		char[] chars = in.toCharArray();
		char quoteChar = 0;

		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];

			if (insideQuotes) {
				if (c == quoteChar) {
					if(i == 0 || chars[i-1] != '\\'){//if previous char is NOT escaped
						insideQuotes = false;
					}
				}
				currentPart.append(c);
			} else {
				if (c == '\'' || c == '"') {
					if(i == 0 || chars[i-1] != '\\'){//if previous char is NOT escaped
						insideQuotes = true;
						quoteChar = c;
					}
					currentPart.append(c);
				} else if (String.valueOf(c).equals(filter)) {
					result.add(currentPart.toString());
					currentPart.setLength(0);
				} else {
					currentPart.append(c);
				}
			}
		}

		// Add the last part
		result.add(currentPart.toString());

		return result;
	}
}
