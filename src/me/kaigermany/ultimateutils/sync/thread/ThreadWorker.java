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
		try {
			while (isAlive) {
				AsyncRunnable r = queue.poll();
				if (r == null) {
					synchronized (ThreadWorker.this) {
						isWorking = false;
					}
				}

				if (r == null) {
					break;
				} else {
					r.execute();
					r = null;
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
