package me.kaigermany.ultimateutils.sync.thread;

/*
 * Launches a background thread and executes the given function.
 * state can be checked with isFinished().
 * final result can be queried with getResult().
 */
public class CoProcess <T> {
	private static long idCounter = 0;
	
	//creates unique Thread names.
	private static String nextThreadName(){
		long id;
		synchronized (CoProcess.class) {
			id = idCounter;
			idCounter++;
		}
		return "CoProcess_" + id;
	}
	
	private final Thread thread;
	private volatile boolean isDone = false;
	private volatile T result;
	private volatile Exception errorResult;
	private final ThreadLock getterLock = new ThreadLock(true);
	
	/**
	 * Setup a new instance.
	 * the internal Thread will start its work immediately.
	 * @param func
	 * 		the function that has to be executed.
	 */
	public CoProcess(final CoProcessSupplier<T> func){
		thread = new Thread(()->{
			try{
				//execute the function and ensure we keep the exception if thrown.
				try{
					result = func.get();
				}catch(Exception e){
					errorResult = e;
				}
				
			} finally {
				synchronized (this) {
					isDone = true;
				}
				getterLock.unlock();
			}
		}, nextThreadName());
		thread.setDaemon(true);
		thread.start();
	}
	
	/**
	 * Check Thread state.
	 * @return if the thread has finished.
	 */
	public boolean isFinished(){
		synchronized (this) {
			return isDone;
		}
	}
	
	/**
	 * returns the produced Object.
	 * Note: the method will block until the thread is done 
	 * to ensure the state of the returned Object remain consistent.
	 * 
	 * @return the Object produced by the Supplier function, including NULL.
	 * @throws Exception the collected Exception, if any.
	 */
	public T getResult() throws Exception {
		getterLock.enterBlock();
		
		if(errorResult != null) {
			throw errorResult;
		}
		
		return result;
	}
}
