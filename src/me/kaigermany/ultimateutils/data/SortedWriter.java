package me.kaigermany.ultimateutils.data;

import java.util.HashMap;
import java.util.function.BiConsumer;

public class SortedWriter <T> {
	private final HashMap<Long, T> buffer = new HashMap<Long, T>();
	private final BiConsumer<Long, T> sortedCallback;
	private volatile long lastIdFlushed = 0;
	
	public SortedWriter(BiConsumer<Long, T> sortedCallback){
		this.sortedCallback = sortedCallback;
	}
	
	public void write(long id, T data){
		synchronized (buffer) {
			buffer.put(id, data);
		}
		flush();
	}
	
	public boolean isEmpty(){
		return getBufferSize() == 0;
	}
	
	public int getBufferSize(){
		int size;
		synchronized (buffer) {
			size = buffer.size();
		}
		return size;
	}

	private void flush() {
		synchronized (sortedCallback) {
			while(true){
				boolean hasNext;
				T data = null;
				long id = 0;
				synchronized (buffer) {
					hasNext = buffer.containsKey(lastIdFlushed);
					if(hasNext){
						data = buffer.remove(lastIdFlushed);
						id = lastIdFlushed;
						lastIdFlushed++;
					}
				}
				if(hasNext){
					sortedCallback.accept(id, data);
				} else {
					break;
				}
			}
		}
	}
}
