package me.kaigermany.ultimateutils.sync.thread;

public abstract class AsyncRunnable implements Runnable {
	public static AsyncRunnable fromRunnable(final Runnable r){
		return new AsyncRunnable() {
			@Override
			public void run() {
				r.run();
			}
		};
	}
	
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
