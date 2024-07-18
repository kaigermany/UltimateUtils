package me.kaigermany.ultimateutils.sync.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class ProcessorQueue {
	private Queue<AsyncRunnable> queue = new LinkedBlockingQueue<>();
	private List<CoProcessorState> notifyList = new ArrayList<>();
	private ThreadLock awaitLock = new ThreadLock();
	
	public void add(AsyncRunnable runnable){
		awaitLock.lock();
		synchronized (queue) {
			queue.add(runnable);
		}
	}
	
	public void poll(Consumer<AsyncRunnable> feedMeCallback){
		boolean empty;
		synchronized (queue) {
			feedMeCallback.accept(queue.poll());
			empty = queue.isEmpty();
		}
		if(empty){
			awaitLock.unlock();
		}
	}
	
	public void addEventListener(CoProcessorState listener){
		synchronized (notifyList) {
			notifyList.add(listener);
		}
	}
	
	public void removeEventListener(CoProcessorState listener){
		synchronized (notifyList) {
			notifyList.remove(listener);
		}
	}

	public void awaitDone() {
		/*
		boolean loop = true;
		while(loop){
			synchronized (queue) {
				loop = queue.peek() != null;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		*/
		awaitLock.enterBlock();
	}
}
