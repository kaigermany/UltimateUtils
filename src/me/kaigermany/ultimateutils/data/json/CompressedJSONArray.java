package me.kaigermany.ultimateutils.data.json;

import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;

public class CompressedJSONArray extends JSONArray {
	public static JSONArray tryCreateCompressed(JSONArray baseArray) {
		int globalTypeID = 0;
		//phase 1: Number-test.
		final int len = baseArray.size();
		for(int i=0; i<len; i++){
			Object entry = baseArray.get(i);
			if(entry == null) return null;
			if(!(entry instanceof Number)) return null;
			int typeID = 0;
			if(entry instanceof Double){
				typeID = 2;
			} else if(entry instanceof Long || entry instanceof Integer){
				typeID = 1;
			}
			if(typeID == 0) return null;
			if(globalTypeID == 0){
				globalTypeID = typeID;
			} else if(globalTypeID != typeID) {
				return null;
			}
		}
		if(globalTypeID == 2){//if double only:
			double[] plainArray = new double[len];
			for(int i=0; i<len; i++){
				plainArray[i] = (Double)baseArray.get(i);
			}
			return new GenericDoubleJSONArray(plainArray);
		}
		
		//phase 2: range test.
		long[] plainLongArray = new long[len];
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		for(int i=0; i<len; i++){
			long val = plainLongArray[i] = ((Number)baseArray.get(i)).longValue();
			if(min > val) min = val;
			if(max < val) max = val;
		}
		byte bmin = (byte)min;
		byte bmax = (byte)max;
		int imin = (int)min;
		int imax = (int)max;
		
		if(bmin == min && bmax == max) {
			byte[] buffer = new byte[len];
			for(int i=0; i<len; i++){
				buffer[i] = (byte)plainLongArray[i];
			}
			return new GenericByteJSONArray(buffer);
		} else if(imin == min && imax == max) {
			int[] buffer = new int[len];
			for(int i=0; i<len; i++){
				buffer[i] = (int)plainLongArray[i];
			}
			return new GenericIntJSONArray(buffer);
		} else {
			return new GenericLongJSONArray(plainLongArray);
		}
	}
	
	
	public static class MethodNotSupportedException extends RuntimeException {
		private static final long serialVersionUID = 9070820628490579211L;
	}
	
	
	@Override
	public boolean getBoolean(int index) {
		throw new MethodNotSupportedException();
	}
	@Override
	public boolean getBoolean(int index, boolean defaultValue) {
		throw new MethodNotSupportedException();
	}
	@Override
	public boolean[] toBooleanArray() {
		throw new MethodNotSupportedException();
	}
	
	
	
	protected CompressedJSONArray() {}

	public CompressedJSONArray(Reader reader) {
		throw new MethodNotSupportedException();
	}

	public CompressedJSONArray(InputStream inputStream) {
		throw new MethodNotSupportedException();
	}

	public CompressedJSONArray(InputStream inputStream, boolean readOnly) {
		throw new MethodNotSupportedException();
	}
	
	protected CompressedJSONArray(JSONTokener x) {
		throw new MethodNotSupportedException();
	}
	
	protected CompressedJSONArray(Object array) {
		throw new MethodNotSupportedException();
	}
	
	@Override
	public String getString(int index) {
		throw new MethodNotSupportedException();
	}
	@Override
	public String getString(int index, String defaultValue) {
		throw new MethodNotSupportedException();
	}

	
	@Override
	public JSONArray getJSONArray(int index) {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONArray getJSONArray(int index, JSONArray defaultValue) {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONObject getJSONObject(int index) {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONObject getJSONObject(int index, JSONObject defaultValue) {
		throw new MethodNotSupportedException();
	}
	@Override
	public String[] toStringArray() {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONObject[] toJSONObjectArray() {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONArray[] toJSONArrayArray() {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONArray add(Object value) {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONArray setString(int index, String value) {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONArray setInt(int index, int value) {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONArray setLong(int index, long value) {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONArray setFloat(int index, float value) {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONArray setDouble(int index, double value) {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONArray setBoolean(int index, boolean value) {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONArray setJSONArray(int index, JSONArray value) {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONArray setJSONObject(int index, JSONObject value) {
		throw new MethodNotSupportedException();
	}
	@Override
	public JSONArray set(int index, Object value) {
		throw new MethodNotSupportedException();
	}
	@Override
	public Object remove(int index) {
		throw new MethodNotSupportedException();
	}
	@Override
	public boolean isNull(int index) {
		return false;
	}

	@Override
	public String toString() {
		return format(2);
	}
	@Override
	public String toJSONString(){
		return format(0);
	}
	
	@Override
	public int getInt(int index, int defaultValue) {
		return getInt(index);
	}
	@Override
	public long getLong(int index, long defaultValue) {
		return getLong(index);
	}
	@Override
	public float getFloat(int index, float defaultValue) {
		return getFloat(index);
	}
	@Override
	public double getDouble(int index, double defaultValue) {
		return getDouble(index);
	}
	@Override
	public float getFloat(int index) {
		return (float) getDouble(index);
	}
	
	public static class GenericDoubleJSONArray extends CompressedJSONArray {
		private final double[] array;
		public GenericDoubleJSONArray(double[] array) {
			this.array = array;
		}
		
		public Object get(int index) {
			return array[index];
		}

		public int size() {
			return array.length;
		}
		
		public String format(int indentFactor) {
			return Arrays.toString(array);
		}
		
		public int getInt(int index) {
			return (int) array[index];
		}
		
		public long getLong(int index) {
			return (long) array[index];
		}
		
		public double getDouble(int index) {
			return array[index];
		}
	}

	public static class GenericByteJSONArray extends CompressedJSONArray {
		private final byte[] array;
		public GenericByteJSONArray(byte[] array) {
			this.array = array;
		}
		
		public Object get(int index) {
			return (int)array[index];
		}

		public int size() {
			return array.length;
		}
		
		public String format(int indentFactor) {
			return Arrays.toString(array);
		}
		
		public int getInt(int index) {
			return array[index];
		}
		
		public long getLong(int index) {
			return array[index];
		}
		
		public double getDouble(int index) {
			return array[index];
		}
	}
	
	public static class GenericIntJSONArray extends CompressedJSONArray {
		private final int[] array;
		public GenericIntJSONArray(int[] array) {
			this.array = array;
		}
		
		public Object get(int index) {
			return array[index];
		}

		public int size() {
			return array.length;
		}
		
		public String format(int indentFactor) {
			return Arrays.toString(array);
		}
		
		public int getInt(int index) {
			return array[index];
		}
		
		public long getLong(int index) {
			return array[index];
		}
		
		public double getDouble(int index) {
			return array[index];
		}
	}
	
	public static class GenericLongJSONArray extends CompressedJSONArray {
		private final long[] array;
		public GenericLongJSONArray(long[] array) {
			this.array = array;
		}
		
		public Object get(int index) {
			return array[index];
		}

		public int size() {
			return array.length;
		}
		
		public String format(int indentFactor) {
			return Arrays.toString(array);
		}
		
		public int getInt(int index) {
			return (int) array[index];
		}
		
		public long getLong(int index) {
			return array[index];
		}
		
		public double getDouble(int index) {
			return array[index];
		}
	}
}
