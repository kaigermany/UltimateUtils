package me.kaigermany.ultimateutils.data;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {
	public static <T> ArrayList<T> reverse(List<T> in){
		ArrayList<T> out = new ArrayList<T>(in.size());
		for(int i=in.size()-1; i>=0; i--){
			out.add(in.get(i));
		}
		return out;
	}

	public static <T> ArrayList<T> limit(List<T> in, int limit){
		limit = Math.min(in.size(), limit);
		ArrayList<T> out = new ArrayList<T>(limit);
		for(int i=0; i<limit; i++) {
			out.add(in.get(i));
		}
		return out;
	}
}
