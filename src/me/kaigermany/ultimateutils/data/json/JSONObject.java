package me.kaigermany.ultimateutils.data.json;

/*
 * based on: https://github.com/processing/processing/blob/master/core/src/processing/data/JSONObject.java
 * 
 * - created by JSON.org, 2002
 * - modified by Processing Foundation, 2018
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

import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class JSONObject {
	private final HashMap<String, Object> map = new HashMap<>();

	public JSONObject() {}

	public JSONObject(Reader reader) {
		this(new JSONTokener(reader));
	}

	public JSONObject(InputStream inputStream) {
		this(new JSONTokener(inputStream));
	}

	public JSONObject(String s) {
		this(new JSONTokener(s));
	}

	protected JSONObject(JSONTokener x) {
		char c;
		String key;
		if (x.nextClean() != '{') {
			throw new RuntimeException("A JSONObject text must begin with '{'");
		}
		while (true) {
			c = x.nextClean();
			switch (c) {
				case 0:
					throw new RuntimeException("A JSONObject text must end with '}'");
				case '}':
					return;
				default:
					x.back();
					key = x.nextValue().toString();
			}
			c = x.nextClean();
			if (c == '=') {
				if (x.next() != '>') {
					x.back();
				}
			} else if (c != ':') {
				throw new RuntimeException("Expected a ':' after a key");
			}
			this.putOnce(key, x.nextValue());
			switch (x.nextClean()) {
				case ';':
				case ',':
					if (x.nextClean() == '}') {
						return;
					}
					x.back();
					break;
				case '}':
					return;
				default:
					throw new RuntimeException("Expected a ',' or '}'");
			}
		}
	}

	protected JSONObject(Map<String, Object> map) {
		if (map != null) {
			for (Entry<String, Object> e : map.entrySet()) {
				if (e.getValue() != null) {
					map.put(e.getKey(), wrap(e.getValue()));
				}
			}

		}
	}

	static public JSONObject parse(String source) {
		return new JSONObject(new JSONTokener(source));
	}

	public Object get(String key) {
		return this.map.get(key);
	}

	public String getString(String key) {
		Object object = this.get(key);
		if (object instanceof String) {
			return (String) object;
		}
		throw new IllegalArgumentException("JSONObject[" + JSONTokener.quote(key) + "] is not a string");
	}

	public String getString(String key, String defaultValue) {
		if (this.get(key) == null) {
			return defaultValue;
		}
		return getString(key);
	}

	public int getInt(String key) {
		return (int) getLong(key);
	}

	public long getLong(String key) {
		Object object = map.get(key);
		if (object instanceof Number) {
			return ((Number) object).longValue();
		}
		throw new IllegalArgumentException("JSONObject[" + JSONTokener.quote(key) + "] is not a number.");
	}

	public float getFloat(String key) {
		return (float) getDouble(key);
	}

	public double getDouble(String key) {
		Object object = map.get(key);
		if (object instanceof Number) {
			return ((Number) object).doubleValue();
		}
		throw new IllegalArgumentException("JSONObject[" + JSONTokener.quote(key) + "] is not a number.");
	}

	public boolean getBoolean(String key) {
		Object object = map.get(key);
		if (object instanceof Boolean) {
			return ((Boolean) object).booleanValue();
		}
		throw new IllegalArgumentException("JSONObject[" + JSONTokener.quote(key) + "] is not a boolean.");
	}

	public JSONArray getJSONArray(String key) {
		Object object = map.get(key);
		if (object instanceof JSONArray) {
			return (JSONArray) object;
		}
		throw new IllegalArgumentException("JSONObject[" + JSONTokener.quote(key) + "] is not a JSONArray.");
	}

	public JSONObject getJSONObject(String key) {
		Object object = map.get(key);
		if (object instanceof JSONObject) {
			return (JSONObject) object;
		}
		throw new IllegalArgumentException("JSONObject[" + JSONTokener.quote(key) + "] is not a JSONObject.");
	}

	public boolean containsKey(String key) {
		return map.containsKey(key);
	}

	public boolean containsValue(String key) {
		return map.containsValue(key);
	}

	public boolean isNull(String key) {
		return this.map.get(key) == null;
	}

	public Iterator<String> iterator() {
		return map.keySet().iterator();
	}

	public Set<String> keySet() {
		return map.keySet();
	}

	public Collection<Object> values() {
		return map.values();
	}

	public int size() {
		return this.map.size();
	}

	public JSONObject setString(String key, String value) {
		return put(key, value);
	}

	public JSONObject setInt(String key, int value) {
		return put(key, value);
	}

	public JSONObject setLong(String key, long value) {
		return put(key, value);
	}

	public JSONObject setFloat(String key, float value) {
		return put(key, value);
	}

	public JSONObject setDouble(String key, double value) {
		return put(key, value);
	}

	public JSONObject setBoolean(String key, boolean value) {
		return this.put(key, value ? Boolean.TRUE : Boolean.FALSE);
	}

	public JSONObject setJSONObject(String key, JSONObject value) {
		return put(key, value);
	}

	public JSONObject setJSONArray(String key, JSONArray value) {
		return put(key, value);
	}

	public JSONObject put(String key, Object value) {
		if (key == null) {
			throw new RuntimeException("Null key.");
		}
		if (value == null) {
			map.remove(key);
		}
		JSONTokener.testValidity(value);
		map.put(key, value);
		return this;
	}

	public JSONObject putOnce(String key, Object value) {
		if (key != null && value != null) {
			if (map.get(key) != null) {
				throw new RuntimeException("Duplicate key \"" + key + "\"");
			}
			map.put(key, value);
		}
		return this;
	}

	public Object remove(String key) {
		return this.map.remove(key);
	}

	@Override
	public String toString() {
		return format(2);
	}

	private String format(int indentFactor) {
		StringWriter w = new StringWriter();
		synchronized (w.getBuffer()) {
			JSONTokener.writeInternal(this, w, indentFactor, 0);
		}
		return w.toString();
	}

	@SuppressWarnings("unchecked")
	static protected Object wrap(Object object) {
		try {
			if (object == null) {
				return null;
			}
			if (object instanceof JSONObject || object instanceof JSONArray
					|| object instanceof Byte || object instanceof Character || object instanceof Short
					|| object instanceof Integer || object instanceof Long || object instanceof Boolean
					|| object instanceof Float || object instanceof Double || object instanceof String) {
				return object;
			}

			if (object instanceof Collection || object.getClass().isArray()) {
				return new JSONArray(object);
			}
			if (object instanceof Map) {
				return new JSONObject((Map<String, Object>)object);
			}
			throw new RuntimeException("Only generics, Collections and Maps can be wrapped.");
		} catch (Exception exception) {
			return null;
		}
	}
}
