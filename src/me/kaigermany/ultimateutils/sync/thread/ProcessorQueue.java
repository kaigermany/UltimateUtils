package me.kaigermany.ultimateutils.sync.thread;

import java.util.Iterator;

public class ProcessorQueue {
	private ThreadLock awaitLock = new ThreadLock();
	private Iterator<AsyncRunnable> queue;
	
	public ProcessorQueue(Iterator<AsyncRunnable> queue){
		this.queue = queue;
		awaitLock.lock();
	}
	
	public AsyncRunnable poll(){
		AsyncRunnable runnable = null;
		synchronized (queue) {
			if(queue.hasNext()){
				runnable = queue.next();
			}
		}
		if(runnable == null){
			awaitLock.unlock();
		}
		return runnable;
	}
	
	public void awaitDone() {
		awaitLock.enterBlock();
	}
	
	public boolean isEmpty(){
		boolean empty;
		synchronized (queue) {
			empty = !queue.hasNext();
		}
		return empty;
	}
}
