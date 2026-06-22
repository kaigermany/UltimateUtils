package me.kaigermany.ultimateutils.sync.thread;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class Parallel {
	public static void exec(int numIterations, Consumer<Integer> function){
		int numThreads = Runtime.getRuntime().availableProcessors();
		exec(numIterations, function, numThreads);
	}
	public static void exec(final int numIterations, Consumer<Integer> function, int numThreads){
		QueueIterator iterator = new QueueIterator(numIterations, function);
		exec(iterator, numThreads);
	}
	public static void exec(final List<Runnable> functionList){
		final ArrayList<AsyncRunnable> localList = new ArrayList<AsyncRunnable>(functionList.size());
		for(Runnable r : functionList){
			localList.add(AsyncRunnable.fromRunnable(r));
		}
		int numThreads = Runtime.getRuntime().availableProcessors();
		FiniteIterator<AsyncRunnable> functionIterator = new FiniteIterator<AsyncRunnable>(){
			final int size = functionList.size();
			final Iterator<AsyncRunnable> it = localList.iterator();
			
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}
	
			@Override
			public AsyncRunnable next() {
				return it.next();
			}
	
			@Override
			public int getSize() {
				return size;
			}
		};
		exec(functionIterator, numThreads);
	}
	
	public static void exec(final FiniteIterator<AsyncRunnable> functionIterator){
		int numThreads = Runtime.getRuntime().availableProcessors();
		exec(functionIterator, numThreads);
	}
	
	public static void exec(FiniteIterator<AsyncRunnable> functionIterator, int numThreads){
		if(functionIterator.getSize() > 1024){
			functionIterator = rebundleIteratorToGroupCalls(functionIterator);
		}
		
		
		ThreadWorker[] cpu = new ThreadWorker[numThreads];
		ProcessorQueue queue = new ProcessorQueue(functionIterator);
		
		String prefix = "Parallel_" + System.currentTimeMillis() + "_";
		for(int i=0; i<numThreads; i++){
			(cpu[i] = new ThreadWorker(queue, prefix + i)).notifyStart();
		}
		
		queue.awaitDone();
		
		for(int i=0; i<numThreads; i++){
			cpu[i].awaitIdle();
		}
	}
	
	private static FiniteIterator<AsyncRunnable> rebundleIteratorToGroupCalls(FiniteIterator<AsyncRunnable> functionIterator) {
		int groupSize = (int)Math.sqrt(functionIterator.getSize());
		if(groupSize < 10) return functionIterator;
		//TODO implement
		return functionIterator;
	}

	public static class IterativeRunnable extends AsyncRunnable {
		private int id;
		private Consumer<Integer> function;
		
		public IterativeRunnable(int id, Consumer<Integer> function){
			this.id = id;
			this.function = function;
		}
		
		@Override
		public void run() {
			function.accept(id);
		}
		
	}
	
	public static class QueueIterator implements FiniteIterator<AsyncRunnable> {
		private final Consumer<Integer> function;
		private final int max;
		private volatile int curr = 0;
		
		public QueueIterator(int counterMaxValue, Consumer<Integer> function){
			this.function = function;
			this.max = counterMaxValue;
		}
		
		@Override
		public boolean hasNext() {
			boolean hasNextResult;
			synchronized (this) {
				hasNextResult = curr < max;
			}
			return hasNextResult;
		}
		
		@Override
		public AsyncRunnable next() {
			AsyncRunnable instance;
			synchronized (this) {
				instance = new IterativeRunnable(curr, function);
				curr++;
			}
			return instance;
		}

		@Override
		public int getSize() {
			return max;
		}
	}
}
