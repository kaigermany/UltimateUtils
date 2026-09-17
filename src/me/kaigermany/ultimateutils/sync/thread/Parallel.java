package me.kaigermany.ultimateutils.sync.thread;

import java.util.List;
import java.util.function.Consumer;

public class Parallel {
	public static void exec(int numIterations, Consumer<Integer> function){
		int numThreads = Runtime.getRuntime().availableProcessors();
		exec(numIterations, function, numThreads);
	}
	public static void exec(final int numIterations, Consumer<Integer> function, int numThreads){
		IndexIterator iterator = new IndexIterator(numIterations, function);
		exec(iterator, numThreads);
	}
	public static void exec(final List<Runnable> functionList){
		int numThreads = Runtime.getRuntime().availableProcessors();
		exec(functionList, numThreads);
	}
	public static void exec(final List<Runnable> functionList, int numThreads){
		IndexIterator iterator = new IndexIterator(functionList.size(), new Consumer<Integer>(){
			@Override
			public void accept(Integer index) {
				//AsyncRunnable.fromRunnable(functionList.get(index)).execute();
				functionList.get(index).run();
			}
		});
		exec(iterator, numThreads);
	}
	
	public static void exec(final FiniteIterator<AsyncRunnable> functionIterator){
		int numThreads = Runtime.getRuntime().availableProcessors();
		exec(functionIterator, numThreads);
	}
	
	public static void exec(FiniteIterator<AsyncRunnable> functionIterator, int numThreads){
		if(functionIterator.getSize() > 1024 && functionIterator instanceof IndexIterator){
			functionIterator = ((IndexIterator)functionIterator).createMultiExecutionIterator(numThreads);
					//rebundleIteratorToGroupCalls(functionIterator);
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
	
	public static class IndexIterator implements FiniteIterator<AsyncRunnable> {
		private final Consumer<Integer> function;
		private final int max;
		private volatile int curr = 0;
		
		public IndexIterator(int counterMaxValue, Consumer<Integer> function){
			this.function = function;
			this.max = counterMaxValue;
		}
		
		public FiniteIterator<AsyncRunnable> createMultiExecutionIterator(int numThreads) {
			return new MultiExecutionIterator(max, function, numThreads);
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
	
	public static class MultiExecutionIterator implements FiniteIterator<AsyncRunnable> {
		private final Consumer<Integer> function;
		private final int max;
		private final int numThreads;
		private volatile int curr = 0;
		
		public MultiExecutionIterator(int counterMaxValue, Consumer<Integer> function, int numThreads){
			this.function = function;
			this.max = counterMaxValue;
			this.numThreads = numThreads;
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
				int remaining = max - curr;
				int localLength = remaining / 2 / numThreads;
				if(localLength <= 1){
					instance = new IterativeRunnable(curr, function);
					curr++;
				} else {
					instance = new MultiIterativeRunnable(function, curr, localLength);
					curr += localLength;
				}
			}
			return instance;
		}

		@Override
		public int getSize() {
			return max;
		}
	}

	public static class MultiIterativeRunnable extends AsyncRunnable {
		private Consumer<Integer> function;
		private int offset;
		private int length;
		
		public MultiIterativeRunnable(Consumer<Integer> function, int offset, int length) {
			this.function = function;
			this.offset = offset;
			this.length = length;
		}

		@Override
		public void run() {
			for(int i=0; i<length; i++){
				try{
					function.accept(offset + i);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
}
