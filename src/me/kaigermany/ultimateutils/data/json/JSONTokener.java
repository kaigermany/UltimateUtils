package me.kaigermany.ultimateutils.data.json;

/*
 * based on: https://github.com/processing/processing/blob/master/core/src/processing/data/JSONTokener.java
 * 
 * - created by JSON.org, 2002
 * - modified by Processing Foundation, 2014
 * - modified by KaiGermany, 2025
 */

/*
Copyright (c) 2002 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class JSONTokener {
	private boolean eof;
	private long index;
	private char previous;
	private Reader reader;
	private boolean usePrevious;

	private Map<Object, Object> deduplicationMap;

	public JSONTokener(Reader reader) {
		this.reader = reader;
		// reader.markSupported() ? reader : new BufferedReader(reader);
		this.eof = false;
		this.usePrevious = false;
		this.previous = 0;
		this.index = 0;
		this.deduplicationMap = new HashMap<Object, Object>();
	}

	public JSONTokener(InputStream inputStream) {
		this(new InputStreamReader(inputStream));
	}

	public JSONTokener(String s) {
		this(new StringReader(s));
	}

	public void back() {
		if (this.usePrevious || this.index <= 0) {
			throw new IllegalStateException("Stepping back two steps is not supported");
		}
		this.index -= 1;
		this.usePrevious = true;
		this.eof = false;
	}

	private boolean end() {
		return this.eof && !this.usePrevious;
	}

	public char next() {
		if (this.usePrevious) {
			this.usePrevious = false;
			this.index++;
			return this.previous;
		}
		
		try {
			int c = this.reader.read();
			
			if (c < 0) { // End of stream
				this.eof = true;
				c = 0;
			}
			
			this.index++;
			return (this.previous = (char) c);
			
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private String next(int n) {
		if (n == 0) {
			return "";
		}
		char[] chars = new char[n];

		for (int pos = 0; pos < n; pos++) {
			chars[pos] = this.next();
			if (this.end()) {
				throw new IllegalStateException("Substring bounds error");
			}
		}
		return new String(chars);
	}

	public char nextClean() {
		while (true) {
			char c = this.next();
			if (c == 0 || c > ' ') {
				return c;
			}
		}
	}

	private String nextString(char quote) {
		char c;
		StringBuilder sb = new StringBuilder();
		while (true) {
			c = this.next();
			switch (c) {
				case 0:
				case '\n':
				case '\r':
					throw new RuntimeException("Unterminated string");
				case '\\':
					c = this.next();
					switch (c) {
					case 'b':
						sb.append('\b');
						break;
					case 't':
						sb.append('\t');
						break;
					case 'n':
						sb.append('\n');
						break;
					case 'f':
						sb.append('\f');
						break;
					case 'r':
						sb.append('\r');
						break;
					case 'u':
						sb.append((char) Integer.parseInt(this.next(4), 16));
						break;
					case '"':
					case '\'':
					case '\\':
					case '/':
						sb.append(c);
						break;
					default:
						throw new RuntimeException("Illegal escape.");
					}
					break;
				default:
					if (c == quote) {
						return deduplicate(sb.toString());
					}
					sb.append(c);
			}
		}
	}

	public Object nextValue() {
		char c = this.nextClean();

		switch (c) {
			case '"':
			case '\'':
				return this.nextString(c);
			case '{':
				this.back();
				return new JSONObject(this);
			case '[':
				this.back();
				return new JSONArray(this);
		}
		StringBuilder sb = new StringBuilder();
		while (c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
			sb.append(c);
			c = this.next();
		}
		this.back();

		String string = sb.toString().trim();
		if (string.isEmpty()) {
			throw new RuntimeException("Missing value");
		}
		return deduplicate(stringToValue(string));
	}

	private static Object stringToValue(String string) {
		if (string.equals("")) {
			return string;
		}
		if (string.equalsIgnoreCase("true")) {
			return Boolean.TRUE;
		}
		if (string.equalsIgnoreCase("false")) {
			return Boolean.FALSE;
		}
		if (string.equalsIgnoreCase("null")) {
			return null;
		}
		char b = string.charAt(0);
		if ((b >= '0' && b <= '9') || b == '.' || b == '-' || b == '+') {
			try {
				if (string.indexOf('.') > -1 || string.indexOf('e') > -1 || string.indexOf('E') > -1) {
					Double d = Double.valueOf(string);
					if (Double.isFinite(d)) {
						return d;
					}
				} else {
					Long myLong = Long.valueOf(string);
					if (myLong.longValue() == myLong.intValue()) {
						return Integer.valueOf(myLong.intValue());
					} else {
						return myLong;
					}
				}
			} catch (Exception ignore) {
			}
		}
		return string;
	}
	
	@Override
	public String toString() {
		return "";
	}

	@SuppressWarnings("unchecked")
	public <T> T deduplicate(T obj) {
		T val;
		if ((val = (T) deduplicationMap.get(obj)) != null) {
			return val;
		}
		deduplicationMap.put(obj, obj);
		return obj;
	}
	
	public static void testValidity(Object o) {
		if (o != null) {
			if (o instanceof Double) {
				if (((Double) o).isInfinite() || ((Double) o).isNaN()) {
					throw new IllegalArgumentException("JSON does not allow non-finite numbers.");
				}
			} else if (o instanceof Float) {
				if (((Float) o).isInfinite() || ((Float) o).isNaN()) {
					throw new IllegalArgumentException("JSON does not allow non-finite numbers.");
				}
			}
		}
	}
}
