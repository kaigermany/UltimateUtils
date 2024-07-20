package me.kaigermany.ultimateutils.sync.thread;

public abstract class AsyncRunnable implements Runnable {
	private volatile boolean done = false;
	
	public final void execute(){
		try{
			run();
		}catch(Exception e){
			e.printStackTrace();
		}
		done = true;
	}
	
	public final boolean isDone(){
		return done;
	}
}
