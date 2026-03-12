package me.kaigermany.ultimateutils.data;

public class Pair<T1, T2> {
	private T1 obj1;
	private T2 obj2;
	public Pair(T1 obj1, T2 obj2){
		this.obj1 = obj1;
		this.obj2 = obj2;
	}
	
	public T1 getFirst(){
		return obj1;
	}
	
	public T2 getSecond(){
		return obj2;
	}
	
	@Override
	public String toString(){
		return "{" + obj1 + " <=> " + obj2 + "}";
	}
}