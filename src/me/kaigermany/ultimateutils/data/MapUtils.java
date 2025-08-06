package me.kaigermany.ultimateutils.data;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MapUtils {
	public static <A, B> List<Map.Entry<A, B>> sortByKey(Map<A, B> map){
		return sort(map, new Comparator<Map.Entry<A, B>>(){
			@Override
			public int compare(Entry<A, B> a, Entry<A, B> b) {
				return compareObjects(a.getKey(), b.getKey());
			}
		});
	}
	
	public static <A, B> List<Map.Entry<A, B>> sortByValue(Map<A, B> map){
		return sort(map, new Comparator<Map.Entry<A, B>>(){
			@Override
			public int compare(Entry<A, B> a, Entry<A, B> b) {
				return compareObjects(a.getValue(), b.getValue());
			}
		});
	}
	
	public static <A, B> List<Map.Entry<A, B>> sort(Map<A, B> map, Comparator<Map.Entry<A, B>> comparator){
		Map.Entry<A, B>[] entries = getEntriesAsArray(map);
		
		Arrays.parallelSort(entries, comparator);
		
		return Arrays.asList(entries);
	}

	@SuppressWarnings("unchecked")
	private static int compareObjects(Object refA, Object refB){
		if(refA != null && refA instanceof Comparable){
			return ((Comparable<Object>)refA).compareTo(refB);
		} else {
			return String.valueOf(refA).compareTo(String.valueOf(refB));
		}
	}
	
	private static <A, B> Map.Entry<A, B>[] getEntriesAsArray(Map<A, B> map){
		@SuppressWarnings("unchecked")
		Map.Entry<A, B>[] entries = new Map.Entry[map.size()];
		int i = 0;
		for(Map.Entry<A, B> e : map.entrySet()){
			entries[i++] = e;
		}
		return entries;
	}
}
