package me.kaigermany.ultimateutils.sync.thread;

import java.util.ArrayList;
//TODO test
public class ThreadLock {
	private volatile boolean lock = false;
	private volatile ArrayList<Thread> lockedThreads = new ArrayList<Thread>();
	
	public ThreadLock(){}
	
	public void lock(){
		synchronized (lockedThreads) {
			lock = true;
		}
	}
	
	public void unlock(){
		ArrayList<Thread> list;
		synchronized (lockedThreads) {
			lock = false;
			list = new ArrayList<Thread>(lockedThreads);
			lockedThreads.clear();
		}
		for(Thread t : list){
			t.interrupt();
		}
	}
	
	public void enterBlock(){
		if(!lock) return;
		synchronized (lockedThreads) {
			lockedThreads.add(Thread.currentThread());
		}
		try{
			while(true){
				Thread.sleep(Integer.MAX_VALUE);
			}
		}catch(InterruptedException e){}
	}
}
