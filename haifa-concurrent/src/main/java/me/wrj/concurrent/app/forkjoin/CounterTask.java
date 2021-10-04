package me.wrj.concurrent.app.forkjoin;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;

import java.util.*;
import java.util.concurrent.*;

public class CounterTask extends RecursiveTask<Long> {

	private static final long serialVersionUID = -4611132178275624069L;

	private static final int THRESHOLD = 100;

	private int begin;

	private int end;

	public CounterTask(int begin, int end) {
		this.begin = begin;
		this.end = end;

	}

	@Override
	protected Long compute() {
		long sum = 0;

		boolean canCompute = (end - begin) <= THRESHOLD;
		if (canCompute) {
			for (int i = begin; i <= end; i++) {
				sum += i;
			}
		} else {
			//大任务拆封成更小的子任务
			int middle = begin + (end - begin) / 2;
			CounterTask leftCount = new CounterTask(begin, middle);
			CounterTask rightCount = new CounterTask(middle + 1, end);
			//子任务执行
			leftCount.fork();
			rightCount.fork();
			
			//等待子任务执行结果
			long left = leftCount.join();
			long right = rightCount.join();
			
			//合并计算结果
			sum = left + right;

		}
		System.out.printf("当前线程是 %s, begin = %d,end = %d,sum = %d \n",Thread.currentThread().getName(),begin,end,sum);
		try {
			Thread.sleep(1000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return sum;
	}

	public static void main(String[] args) throws Exception{

		CounterTask task = new CounterTask(1, 10000);
		ForkJoinPool pool = new ForkJoinPool();
		Future<Long> result = pool.submit(task);
		if(task.isCompletedAbnormally()){
			System.out.println("异常啦");
		}
		try {
			System.out.println("最后求值是:"+result.get());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
