package me.kaigermany.ultimateutils.sync.thread;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CoProcessor {
	private static CoProcessor globalInstance = null;
	
	private final int numThreads;
	private final ThreadWorker[] cpu;
	private final LinkedBlockingQueue<AsyncRunnable> queueBuffer = new LinkedBlockingQueue<>();
	private final ProcessorQueue queue = new ProcessorQueue(new Iterator<AsyncRunnable>() {
		@Override
		public AsyncRunnable next() {
			AsyncRunnable instance;
			synchronized (queueBuffer) {
				instance = queueBuffer.poll();
			}
			return instance;
		}
		
		@Override
		public boolean hasNext() {
			boolean next;
			synchronized (queueBuffer) {
				next = queueBuffer.peek() != null;
			}
			return next;
		}
	});
	private static final String threadNamePrefix = "CoProcessor_";

	public static CoProcessor getInstance(){
		CoProcessor instance;
		synchronized (CoProcessor.class) {
			instance = globalInstance;
			if(instance == null){
				instance = globalInstance = new CoProcessor();
			}
		}
		return instance;
	}
	public static CoProcessor createInstance(){
		return new CoProcessor();
	}
	public static CoProcessor createInstance(int numWorkerThreads){
		return new CoProcessor(numWorkerThreads);
	}
	
	private void triggerStart(){
		for(int i=0; i<numThreads; i++){
			if(cpu[i] == null) cpu[i] = new ThreadWorker(queue, threadNamePrefix + i);
			cpu[i].notifyStart();
		}
	}
	
	private HashSet<AsyncRunnable> activeJobs = new HashSet<>();
	private ThreadLock awaitLock = new ThreadLock();

	private CoProcessor(){
		this.numThreads = Runtime.getRuntime().availableProcessors();
		this.cpu = new ThreadWorker[numThreads];
	}
	private CoProcessor(int numThreads){
		this.numThreads = numThreads;
		this.cpu = new ThreadWorker[numThreads];
	}
	
	public void awaitAllJobs(){
		awaitLock.enterBlock();
	}
	
	public CoProcessor putJob(Runnable simpleJob){
		awaitLock.lock();
		AsyncRunnable r = new AsyncRunnable() {
			@Override
			public void run() {
				try{
					simpleJob.run();
				} finally {
					finishJob(this);
				}
			}
		};
		synchronized (activeJobs) {
			activeJobs.add(r);
		}
		synchronized (queueBuffer) {
			queueBuffer.add(r);
		}
		triggerStart();
		return this;
	}
	
	public CoProcessor putIterativeJob(int numIterations, Consumer<Integer> function){
		awaitLock.lock();
		ArrayList<AsyncRunnable> jobList = new ArrayList<>(numIterations);
		for(int i=0; i<numIterations; i++){
			final int ii = i;
			jobList.add(new AsyncRunnable() {
				final int index = ii;
				@Override
				public void run() {
					try{
						function.accept(index);
					} finally {
						finishJob(this);
					}
				}
			});
		}
		addJobsInternal(jobList);
		return this;
	}
	
	public <T> CoProcessor putListJob(List<T> list, BiConsumer<T, Integer> function){
		awaitLock.lock();
		ArrayList<AsyncRunnable> jobList = new ArrayList<>(list.size());
		for(int i=0; i<list.size(); i++){
			final int ii = i;
			final T obj = list.get(i);
			jobList.add(new AsyncRunnable() {
				final int index = ii;
				final T object = obj;
				@Override
				public void run() {
					try{
						function.accept(object, index);
					} finally {
						finishJob(this);
					}
				}
			});
		}
		addJobsInternal(jobList);
		return this;
	}
	
	public CoProcessor putIteratorJob(Iterable<Runnable> function){
		return putIteratorJob(function.iterator());
	}
	
	public CoProcessor putIteratorJob(Iterator<Runnable> function){
		awaitLock.lock();
		ArrayList<AsyncRunnable> jobList = new ArrayList<>();
		while(function.hasNext()){
			final Runnable runnable = function.next();
			jobList.add(new AsyncRunnable() {
				final Runnable func = runnable;
				@Override
				public void run() {
					try{
						func.run();
					} finally {
						finishJob(this);
					}
				}
			});
		}
		addJobsInternal(jobList);
		return this;
	}
	
	public int getQueueSize(){
		int size;
		synchronized (activeJobs) {
			size = activeJobs.size();
		}
		return size;
	}
	
	private void addJobsInternal(ArrayList<AsyncRunnable> jobList){
		synchronized (activeJobs) {
			activeJobs.addAll(jobList);
		}
		synchronized (queueBuffer) {
			queueBuffer.addAll(jobList);
		}
		triggerStart();
	}
	
	private void finishJob(AsyncRunnable r){
		boolean done;
		synchronized (activeJobs) {
			activeJobs.remove(r);
			done = activeJobs.isEmpty();
		}
		if(done){
			awaitLock.unlock();
		}
	}
}
