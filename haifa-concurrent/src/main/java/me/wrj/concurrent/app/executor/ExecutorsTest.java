package me.wrj.concurrent.app.executor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class ExecutorsTest {


	public static void main(String[] args) {
		
		//ExecutorService exec1 = Executors.newSingleThreadExecutor();
		//ExecutorService exec1 = Executors.newFixedThreadPool(5);
		ScheduledExecutorService exec1 =Executors.newScheduledThreadPool(4);

		BlockingQueue<Runnable> workQueue   = new ArrayBlockingQueue<Runnable>(32);

		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(8,16,10,TimeUnit.SECONDS,
				workQueue,Executors.defaultThreadFactory(),new WarnAndDiscardHandler());

		exec1.scheduleAtFixedRate(new ReportThreaPoolInfo(threadPoolExecutor),0L,2L,TimeUnit.SECONDS);

	    while (true) {
	    	try {
				threadPoolExecutor.submit(new MyTask());
				Thread.sleep(500L);
			}catch (Exception e) {
	    		e.printStackTrace();
			}

		}

	}
	
	
}

class ReportThreaPoolInfo implements  Runnable{

	private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;


	ThreadPoolExecutor threadPoolExecutor;

	ReportThreaPoolInfo(ThreadPoolExecutor threadPoolExecutor) {
		this.threadPoolExecutor = threadPoolExecutor;
	}


	@Override
	public void run() {
		System.out.printf("时间 %s,线程-%s,ActiveCount=%d,corePoolSize=%d,poolSize=%d,taskCount=%d, completedTaskCount=%d,workQueue.size=%d \r\n",
				LocalDateTime.now().format(formatter),Thread.currentThread().getName(), threadPoolExecutor.getActiveCount()
				,threadPoolExecutor.getCorePoolSize(),threadPoolExecutor.getPoolSize(),threadPoolExecutor.getTaskCount(),
				threadPoolExecutor.getCompletedTaskCount(),threadPoolExecutor.getQueue().size());



	}
}

class MyTask implements Runnable{
	private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	@Override
	public void run() {
		try {
			Random random = new Random();
			long sleepSeconds = 1000 * (random.nextInt(5)+1) ;
			System.out.println("时间: "+LocalDateTime.now().format(formatter)+" 线程: "+Thread.currentThread().getName()+" sleep "+sleepSeconds+"毫秒");

			List<byte[]>  byteList = new ArrayList<>();
			for(int i = 0; i < 200; i++){
				byteList.add(new byte[1024*1024]);
			}

			int k = 0;
			while (k < 1000000) {
				k++;
			}

			Thread.sleep(sleepSeconds);

		} catch (InterruptedException e) {
			//忽略中断
			e.printStackTrace();
		}
		System.out.println("时间: "+LocalDateTime.now().format(formatter)+": 线程("+Thread.currentThread().getName()+") sleep 结束");

	}
	
}


class WarnAndDiscardHandler implements RejectedExecutionHandler{

	public WarnAndDiscardHandler(){

	}
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

		System.out.printf("任务%s 被线程池%s拒绝\r\n",r.toString(),executor.toString());
	}
}


