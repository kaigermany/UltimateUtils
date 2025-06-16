package me.kaigermany.ultimateutils.sync.thread;

public class ThreadWorker {
	private volatile boolean isWorking = false;
	private volatile boolean isAlive = true;
	private final ProcessorQueue queue;
	private final ThreadLock awaitLock;
	private final String threadName;
	
	public ThreadWorker(ProcessorQueue queue, String threadName){
		this.queue = queue;
		this.threadName = threadName;
		awaitLock = new ThreadLock();
		notifyStart();
	}
	
	public void notifyStart(){
		awaitLock.lock();
		synchronized (ThreadWorker.this) {
			if(!isWorking) {
				Thread thread = new Thread(()->runLoop(), threadName);
				thread.setDaemon(true);
				thread.start();
			}
			isWorking = true;
		}
	}
	
	private void runLoop(){
		final AsyncRunnable[] functionPointer = new AsyncRunnable[1];
		try {
			while (isAlive) {
				queue.poll((func) -> {
					functionPointer[0] = func;
					if (func == null) {
						synchronized (ThreadWorker.this) {
							isWorking = false;
						}
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
