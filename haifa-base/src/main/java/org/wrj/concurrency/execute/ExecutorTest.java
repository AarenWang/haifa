package org.wrj.concurrency.execute;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExecutorTest {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		Callable<Object> task = new Callable<Object>() {
			public Object call() throws Exception
			{
				//Thread.sleep(2000);
				Object result = "This is a Callable test";
				return result;
			}
		};
		
		Future<Object> future = executor.submit(task);
		Object result = future.get();
		System.out.println(result);
	}

}
