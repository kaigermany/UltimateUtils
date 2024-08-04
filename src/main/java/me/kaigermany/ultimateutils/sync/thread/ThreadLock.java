package me.kaigermany.ultimateutils.sync.thread;

import java.util.ArrayList;
//TODO still require some testing...
public class ThreadLock {
	private volatile boolean lock = false;
	private volatile ArrayList<Thread> lockedThreads = new ArrayList<Thread>();
	
	/*
	 * new instance where lock = false;
	 */
	public ThreadLock(){}
	
	/*
	 * new instance where lock = initialLockState;
	 */
	public ThreadLock(boolean initialLockState){
		lock = initialLockState;
	}
	
	/*
	 * once called, all Threads calling enterBlock() will 'freezed' there and wait forever until someone calls unlock().
	 * 	 
	 * method is Threadsave.
	 * method can be called multiple times without taking damage.
	 */
	public void lock(){
		synchronized (lockedThreads) {
			lock = true;
		}
	}
	/*
	 * once called, all Threads freezed in enterBlock() will instantly return,
	 * other Threads that later on calling enterBlock() will also return instantly.
	 * 
	 * method is Threadsave.
	 * method can be called multiple times without taking damage.
	 */
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
	
	/*
	 * Every Thread calling this method will 'freeze' here as long as this ThreadLock instance is in lock-mode.
	 */
	public void enterBlock(){
		synchronized (lockedThreads) {
			if(!lock) return;
			lockedThreads.add(Thread.currentThread());
		}
		awaitUnlock();
	}
	
	/*
	 * returns true if lock() previously called, false otherwise.
	 * if unlock() was called or new instance was created, its default state is false.
	 */
	public boolean isLocked(){
		return lock;//should not require sync because lock is already volatile and here read-only.
	}
	
	/*
	 * separated wail-loop to make Thread-states more clear and visible if 
	 * you inspect the Thread's call-stack.
	 */
	private static void awaitUnlock(){
		try {
			while(true) {
				Thread.sleep(Integer.MAX_VALUE);
			}
		} catch(InterruptedException ignored){}
	}
}
