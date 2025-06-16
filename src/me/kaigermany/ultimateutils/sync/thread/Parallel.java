package me.kaigermany.ultimateutils.sync.thread;

import java.util.Iterator;
import java.util.function.Consumer;

public class Parallel {
	public static void exec(int numIterations, Consumer<Integer> function){
		int numThreads = Runtime.getRuntime().availableProcessors();
		exec(numIterations, function, numThreads);
	}
	public static void exec(final int numIterations, Consumer<Integer> function, int numThreads){
		ThreadWorker[] cpu = new ThreadWorker[numThreads];
		
		ProcessorQueue queue = new ProcessorQueue(new QueueIterator(numIterations, function));
		
		String prefix = "Parallel_" + System.currentTimeMillis() + "_";
		for(int i=0; i<numThreads; i++){
			(cpu[i] = new ThreadWorker(queue, prefix + i)).notifyStart();
		}
		
		queue.awaitDone();
		
		for(int i=0; i<numThreads; i++){
			cpu[i].awaitIdle();
		}
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
	
	public static class QueueIterator implements Iterator<AsyncRunnable> {
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
	}
}
