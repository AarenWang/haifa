package org.wrj.concurrency.execute;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * ʹ�ò�����������鲢���������
 * ʹ�õ������ExecutorService Future Callable
 * @author think
 *
 */
public class ConcurrentCalculator {

	private ExecutorService exec;
	private int cpuCoreNumber;
	private List<Future<Long>> tasks = new ArrayList<Future<Long>>();
	
	class SumCalculator implements Callable<Long>
	{
		private int[] numbers;
		private int start;
		private int end;
		
		public SumCalculator(final int[] numbers,int start,int end)
		{
			this.numbers = numbers;
			this.start = start;
			this.end = end;
		}
		
		public Long call() throws Exception
		{
			System.out.println(this.getClass());
			Long sum = 0L;
			for(int i = start; i < end; i++)
			{
				sum+=numbers[i];
				Thread.sleep(new Random().nextInt(1000));
			}
			return sum;
		}
	}
	
	public ConcurrentCalculator()
	{
		cpuCoreNumber = Runtime.getRuntime().availableProcessors();
		exec = Executors.newFixedThreadPool(cpuCoreNumber);
	}
	
	public Long sum(final int[] numbers)
	{
		for(int i = 0; i < cpuCoreNumber;i++)
		{
			int increment = numbers.length / cpuCoreNumber + 1;
			int start = increment * i;
			int end = increment * i + increment;
			if (end > numbers.length)
			{
				end = numbers.length;
			}
			SumCalculator sumCalc = new SumCalculator(numbers, start, end);
			FutureTask<Long> task = new FutureTask<Long>(sumCalc);
			tasks.add(task);
			if(!exec.isShutdown())
			{
				exec.submit(task);  
			}
		}
		 return getResult();  
	}
	
	public Long getResult()
	{
		Long result = 0l;  
        for (Future<Long> task : tasks) {  
            try {  
                // ������δ���������  
                Long subSum = task.get();  
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
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int[] numbers = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 10, 11 };  
		ConcurrentCalculator calc = new ConcurrentCalculator();  
		Long sum = calc.sum(numbers);  
		System.out.println(sum);  
		calc.close();  

	}

}
