package org.wrj.concurrency.execute;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrentCalculator2 {
	private ExecutorService exec;
	private CompletionService<Long> completionService;

	private int cpuCoreNumber;

	class SumCalculator implements Callable<Long> {
		private int[] numbers;
		private int start;
		private int end;

		public SumCalculator(final int[] numbers, int start, int end) {
			this.numbers = numbers;
			this.start = start;
			this.end = end;
		}

		public Long call() throws Exception {
			System.out.println(this.getClass());
			Long sum = 0L;
			for (int i = start; i < end; i++) {
				sum += numbers[i];
				Thread.sleep(new Random().nextInt(1000));
			}
			return sum;
		}
	}

	public ConcurrentCalculator2() {
		cpuCoreNumber = Runtime.getRuntime().availableProcessors();
		exec = Executors.newFixedThreadPool(cpuCoreNumber);
		completionService = new ExecutorCompletionService<Long>(exec);
	}

	public Long sum(final int[] numbers) {
		// ���CPU���ĸ��������񣬴���FutureTask���ύ��Executor
		for (int i = 0; i < cpuCoreNumber; i++) {
			int increment = numbers.length / cpuCoreNumber + 1;
			int start = increment * i;
			int end = increment * i + increment;
			if (end > numbers.length)
				end = numbers.length;
			SumCalculator subCalc = new SumCalculator(numbers, start, end);
			if (!exec.isShutdown()) {
				completionService.submit(subCalc);

			}
		}
		return getResult();
	}

	public Long getResult() {
		Long result = 0l;
		for (int i = 0; i < cpuCoreNumber; i++) {
			try {
				Long subSum = completionService.take().get();
				result += subSum;
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	public void close() {
		exec.shutdown();
	}
	
	public static void main(String[] args) {
		int[] numbers = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 10, 11 };  
		ConcurrentCalculator calc = new ConcurrentCalculator();  
		Long sum = calc.sum(numbers);  
		System.out.println(sum);  
		calc.close();  

	}

}
