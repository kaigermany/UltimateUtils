package me.kaigermany.ultimateutils.sync.thread;

public class ThreadWorker {
	private Thread thread;
	private ProcessorQueue queue;
	private volatile boolean isWorking = false;
	private ThreadLock awaitLock;
	private volatile boolean isAlive = true;
	
	public ThreadWorker(ProcessorQueue queue){
		this.queue = queue;
		thread = new Thread(()->runLoop());
		thread.setDaemon(true);
		awaitLock = new ThreadLock();
	}
	
	public void notifyStart(){
		awaitLock.lock();
		synchronized (ThreadWorker.this) {
			if(!isWorking) thread.start();
			isWorking = true;
		}
	}
	
	private void runLoop(){
		final AsyncRunnable[] functionPointer = new AsyncRunnable[1];
		while(isAlive){
			queue.poll((func)->{
				if(func == null){
					synchronized (ThreadWorker.this) {
						isWorking = false;
					}
				} else {
					functionPointer[0] = func;
				}
			});
			
			if(functionPointer[0] == null){
				awaitLock.unlock();
				return;
			} else {			
				functionPointer[0].execute();
				functionPointer[0] = null;
			}
		}
	}

	public void awaitIdle() {
		awaitLock.enterBlock();
	}

	public void stop() {
		isAlive = false;
	}
}
