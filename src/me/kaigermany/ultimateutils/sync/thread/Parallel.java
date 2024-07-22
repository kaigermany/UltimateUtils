package me.kaigermany.ultimateutils.sync.thread;

import java.util.Iterator;
import java.util.function.Consumer;

public class Parallel {
	public static void exec(int numIterations, Consumer<Integer> function){
		int numThreads = Runtime.getRuntime().availableProcessors();
		ThreadWorker[] cpu = new ThreadWorker[numThreads];
		ProcessorQueue queue = new ProcessorQueue(new Iterator<AsyncRunnable>() {
			final int max = numIterations;
			volatile int curr;
			
			@Override
			public AsyncRunnable next() {
				AsyncRunnable instance = new IterativeRunnable(curr, function);
				curr++;
				return instance;
			}
			
			@Override
			public boolean hasNext() {
				return curr < max;
			}
		});
		for(int i=0; i<numThreads; i++){
			cpu[i] = new ThreadWorker(queue);
			cpu[i].notifyStart();
		}
		
		queue.awaitDone();
		
		for(int i=0; i<numThreads; i++){
			cpu[i].awaitIdle();
			cpu[i].stop();
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
}
