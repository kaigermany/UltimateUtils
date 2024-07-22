package me.kaigermany.ultimateutils.sync.thread;

import java.util.Iterator;
import java.util.function.Consumer;

public class ProcessorQueue {
	private ThreadLock awaitLock = new ThreadLock();
	private Iterator<AsyncRunnable> queue;
	
	public ProcessorQueue(Iterator<AsyncRunnable> queue){
		this.queue = queue;
		awaitLock.lock();
	}
	
	public void poll(Consumer<AsyncRunnable> feedMeCallback){
		boolean empty;
		synchronized (queue) {
			empty = !queue.hasNext();
			feedMeCallback.accept(empty ? null : queue.next());
		}
		if(empty){
			awaitLock.unlock();
		}
	}
	
	public void awaitDone() {
		awaitLock.enterBlock();
	}
}
