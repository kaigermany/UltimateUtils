package me.kaigermany.ultimateutils.data.generictables;
/*
 * The BitMap basically wraps UncheckedBitMap.java but with boundary checks.
 * If you are already know your bounds and don't need further safety checks,
 * then you can simply skip that part by using UncheckedBitMap.java instead.
 */
public class BitMap {
	private UncheckedBitMap map;
	private int size;
	
	public BitMap(int size){
		this.map = new UncheckedBitMap(size);
		this.size = size;
	}
	
	/**
	 * Wraps an UncheckedBitMap, using the full capacity of its internal array as the size.
	 * If you need a smaller logical size, use BitMap(UncheckedBitMap, int newSize) instead.
	 */
	public BitMap(UncheckedBitMap copyFrom){
		this.map = new UncheckedBitMap(copyFrom);
		this.size = this.map.getDirectMapAccess().length * 32;
	}
	public BitMap(UncheckedBitMap copyFrom, int newSize){
		this.map = new UncheckedBitMap(copyFrom, newSize);
		this.size = newSize;
	}
	public BitMap(BitMap copyFrom){
		this.map = new UncheckedBitMap(copyFrom.map);
		this.size = copyFrom.size;
	}
	public BitMap(BitMap copyFrom, int newSize){
		this.map = new UncheckedBitMap(copyFrom.map, newSize);
		this.size = newSize;
	}
	
	public int get(int index){
		if(index < 0 || index >= size) throw new IllegalArgumentException("Index is out of bounds! Expected 0 .. " + size + ", got " + index);
		return this.map.get(index);
	}
	
	public void set(int index, int bit){//bit MUST be 0 or 1
		if(index < 0 || index >= size) throw new IllegalArgumentException("Index is out of bounds! Expected 0 .. " + size + ", got " + index);
		if((bit & ~1) != 0) throw new IllegalArgumentException("Invalid bit! Expected 0 or 1, got " + bit);
		this.map.set(index, bit);
	}
	
	public void set(int index, boolean bit){
		if(index < 0 || index >= size) throw new IllegalArgumentException("Index is out of bounds! Expected 0 .. " + size + ", got " + index);
		this.map.set(index, bit);
	}
	
	public void fill(int start, int length, int bit){
		if((bit & ~1) != 0) throw new IllegalArgumentException("Invalid bit! Expected 0 or 1, got " + bit);
		fill(start, length, bit == 1);
	}
	
	public void fill(int start, int length, boolean bit){
		if(start < 0 || start >= size) throw new IllegalArgumentException("Start is out of bounds! Expected 0 .. " + size + ", got " + start);
		if(length < 0 || start+length > size) throw new IllegalArgumentException("Length is out of bounds! Expected 0 .. " + (size - start) + ", got " + length);
		this.map.fill(start, length, bit);
	}
	
	public int getSize(){
		return size;
	}
}
