package me.kaigermany.ultimateutils.sync.thread;

public class ThreadWorker {
	private ProcessorQueue queue;
	private volatile boolean isWorking = false;
	private ThreadLock awaitLock;
	private volatile boolean isAlive = true;
	
	public ThreadWorker(ProcessorQueue queue){
		this.queue = queue;
		startThread();
		awaitLock = new ThreadLock();
	}
	
	public void notifyStart(){
		awaitLock.lock();
		synchronized (ThreadWorker.this) {
			if(!isWorking) {
				startThread();
			}
			isWorking = true;
		}
	}
	
	private void startThread(){
		Thread thread = new Thread(()->runLoop());
		thread.setDaemon(true);
		thread.start();
	}
	
	private void runLoop(){
		final AsyncRunnable[] functionPointer = new AsyncRunnable[1];
		try {
			while (isAlive) {
				queue.poll((func) -> {
					if (func == null) {
						synchronized (ThreadWorker.this) {
							isWorking = false;
						}
					} else {
						functionPointer[0] = func;
					}
				});

				if (functionPointer[0] == null) {
					break;
				} else {
					functionPointer[0].execute();
					functionPointer[0] = null;
				}
			}
		} finally {
			awaitLock.unlock();
		}
	}

	public void awaitIdle() {
		awaitLock.enterBlock();
	}

	public void stop() {
		isAlive = false;
	}
}
