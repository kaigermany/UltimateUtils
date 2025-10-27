package me.kaigermany.ultimateutils.data.json;

/*
 * based on: https://github.com/processing/processing/blob/master/core/src/processing/data/JSONArray.java
 * 
 * - created by JSON.org, 2002
 * - modified by Processing Foundation, 2017
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

import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class JSONArray {
  private final ArrayList<Object> myArrayList = new ArrayList<>();

  public JSONArray() {}
  
  public JSONArray(Reader reader) {
    this(new JSONTokener(reader));
  }

	protected JSONArray(JSONTokener x) {
		if (x.nextClean() != '[') {
			throw new IllegalArgumentException("A JSONArray text must start with '['");
		}
		if (x.nextClean() != ']') {
			x.back();
			while (true) {
				if (x.nextClean() == ',') {
					x.back();
					myArrayList.add(null);
				} else {
					x.back();
					myArrayList.add(x.nextValue());
				}
				switch (x.nextClean()) {
				case ';':
				case ',':
					if (x.nextClean() == ']') {
						return;
					}
					x.back();
					break;
				case ']':
					return;
				default:
					throw new IllegalArgumentException("Expected a ',' or ']'");
				}
			}
		}
	}

  static public JSONArray parse(String source) {
      return new JSONArray(new JSONTokener(source));
  }

  protected JSONArray(Object array) {//TODO
    if (array.getClass().isArray()) {
      int length = Array.getLength(array);
      for (int i = 0; i < length; i += 1) {
        this.add(JSONObject.wrap(Array.get(array, i)));
      }
    } else {
      throw new RuntimeException("JSONArray initial value should be a string or collection or array.");
    }
  }

	private Object get(int index) {
		return myArrayList.get(index);
	}

	public String getString(int index) {
		Object object = this.get(index);
		if (object instanceof String) {
			return (String) object;
		}
		throw new RuntimeException("JSONArray[" + index + "] is not a string.");
	}

	public String getString(int index, String defaultValue) {
		String object;
		if ((object = this.getString(index)) != null) return object;
		return defaultValue;
	}

  public int getInt(int index) {
    return (int)getLong(index);
  }

  public int getInt(int index, int defaultValue) {
	    return (int)getLong(index, defaultValue);
  }

  public long getLong(int index) {
    Object object = this.get(index);
    if(object instanceof Number) return ((Number)object).longValue();
     throw new IllegalArgumentException("JSONArray[" + index + "] is not a number.");
  }

  public long getLong(int index, long defaultValue) {
    if(this.get(index) == null) return defaultValue;
      return getLong(index);
  }
  
  public float getFloat(int index) {
    return (float)getDouble(index);
  }

  public float getFloat(int index, float defaultValue) {
	    return (float)getDouble(index, defaultValue);
  }

  public double getDouble(int index) {
	    Object object = this.get(index);
	    if(object instanceof Number) return ((Number)object).doubleValue();
	     throw new IllegalArgumentException("JSONArray[" + index + "] is not a number.");
	     
  }

  public double getDouble(int index, double defaultValue) {
	  if(this.get(index) == null) return defaultValue;
      return getDouble(index);
  }

  public boolean getBoolean(int index) {
    Object object = this.get(index);
    if(object instanceof Boolean) return ((Boolean)object).booleanValue();
    throw new RuntimeException("JSONArray[" + index + "] is not a boolean.");
  }

  public boolean getBoolean(int index, boolean defaultValue)  {
	  if(this.get(index) == null) return defaultValue;
      return getBoolean(index);
  }

  public JSONArray getJSONArray(int index) {
    Object object = this.get(index);
    if (object instanceof JSONArray) {
      return (JSONArray)object;
    }
    throw new RuntimeException("JSONArray[" + index + "] is not a JSONArray.");
  }

  public JSONArray getJSONArray(int index, JSONArray defaultValue) {
	  if(this.get(index) == null) return defaultValue;
      return getJSONArray(index);
  }

  public JSONObject getJSONObject(int index) {
    Object object = this.get(index);
    if (object instanceof JSONObject) {
      return (JSONObject)object;
    }
    throw new RuntimeException("JSONArray[" + index + "] is not a JSONObject.");
  }

  public JSONObject getJSONObject(int index, JSONObject defaultValue) {
	  if(this.get(index) == null) return defaultValue;
      return getJSONObject(index);
  }

  public String[] toStringArray() {
    String[] outgoing = new String[size()];
    for (int i = 0; i < size(); i++) {
      outgoing[i] = getString(i);
    }
    return outgoing;
  }

  public int[] toIntArray() {
    int[] outgoing = new int[size()];
    for (int i = 0; i < size(); i++) {
      outgoing[i] = getInt(i);
    }
    return outgoing;
  }

  public long[] toLongArray() {
    long[] outgoing = new long[size()];
    for (int i = 0; i < size(); i++) {
      outgoing[i] = getLong(i);
    }
    return outgoing;
  }

  public float[] toFloatArray() {
    float[] outgoing = new float[size()];
    for (int i = 0; i < size(); i++) {
      outgoing[i] = getFloat(i);
    }
    return outgoing;
  }

  public double[] toDoubleArray() {
    double[] outgoing = new double[size()];
    for (int i = 0; i < size(); i++) {
      outgoing[i] = getDouble(i);
    }
    return outgoing;
  }

  public boolean[] toBooleanArray() {
    boolean[] outgoing = new boolean[size()];
    for (int i = 0; i < size(); i++) {
      outgoing[i] = getBoolean(i);
    }
    return outgoing;
  }

  public JSONArray add(Object value) {
    myArrayList.add(value);
    return this;
  }

  public JSONArray setString(int index, String value) {
	  return this.set(index, value);
  }

  public JSONArray setInt(int index, int value) {
	  return this.set(index, value);
  }

  public JSONArray setLong(int index, long value) {
	  return this.set(index, value);
  }

  public JSONArray setFloat(int index, float value) { 
	  return this.set(index, value);
  }

  public JSONArray setDouble(int index, double value) {
	  return this.set(index, value);
  }

  public JSONArray setBoolean(int index, boolean value) {
    return set(index, value ? Boolean.TRUE : Boolean.FALSE);
  }

  public JSONArray setJSONArray(int index, JSONArray value) {
	  return this.set(index, value);
  }

  public JSONArray setJSONObject(int index, JSONObject value) {
	  return this.set(index, value);
  }
  
  public JSONArray set(int index, Object value) {
    if (index < 0) {
      throw new IllegalArgumentException("JSONArray[" + index + "] not found.");
    }
    JSONTokener.testValidity(value);
    if (index < myArrayList.size()) {
      myArrayList.set(index, value);
    } else {
      while (index > myArrayList.size()) {
        myArrayList.add(null);
      }
      myArrayList.add(value);
    }
    return this;
  }

  public int size() {
    return myArrayList.size();
  }

  public boolean isNull(int index) {
    return myArrayList.get(index) == null;
  }

  public Object remove(int index) { 
    return myArrayList.remove(index);
  }

  @Override
  public String toString() {
      return format(2);
  }

  private String format(int indentFactor) {
    StringWriter sw = new StringWriter();
    synchronized (sw.getBuffer()) {
    	this.writeInternal(sw, indentFactor, 0);
    }
    return sw.toString();
  }

  protected void writeInternal(StringWriter writer, int indentFactor, int indent) {
      boolean commanate = false;
      int length = this.size();
      writer.write('[');

      if (length == 1) {
			JSONObject.writeValue(writer, myArrayList.get(0), indentFactor, indent);
      } else if (length != 0) {
        final int newIndent = indent + indentFactor;

        for (int i = 0; i < length; i += 1) {
          if (commanate) {
            writer.write(',');
          }
          if (indentFactor != 0) {
            writer.write('\n');
          }
          JSONObject.indent(writer, newIndent);
				JSONObject.writeValue(writer, myArrayList.get(i), indentFactor, newIndent);
          commanate = true;
        }
        if (indentFactor != 0) {
          writer.write('\n');
        }
        JSONObject.indent(writer, indent);
      }
      writer.write(']');
      return;
  }
}
