package me.kaigermany.ultimateutils;

public class PerformanceTimeWatch {
	public static long mesureFunctionRunMS(Runnable function) {
		long time = System.currentTimeMillis();
		function.run();
		return System.currentTimeMillis() - time;
	}
	public static long mesureFunctionRunNANOS(Runnable function) {
		long time = System.nanoTime();
		function.run();
		return System.nanoTime() - time;
	}
	public static void performanceTester(Runnable function, int iterationCycles){
		long[] mesureMap = new long[iterationCycles];
		
		if(mesureFunctionRunMS(function) < 100){
			for(int i=0; i<iterationCycles; i++){
				mesureMap[i] = mesureFunctionRunNANOS(function);
			}
		} else {
			for(int i=0; i<iterationCycles; i++){
				mesureMap[i] = mesureFunctionRunMS(function);
			}
		}
		
		long min, max, avg = 0;
		min = max = mesureMap[0];
		for(int i=0; i<iterationCycles; i++){
			min = Math.min(min, mesureMap[0]);
			max = Math.max(max, mesureMap[0]);
			avg += mesureMap[0];
		}
		avg /= iterationCycles;
		
		System.out.println(min + " " + max + " " + avg);
	}
}
