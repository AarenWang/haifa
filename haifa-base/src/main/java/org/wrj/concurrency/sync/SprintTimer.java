package org.wrj.concurrency.sync;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 短跑计时器，使用CountDownLatch工具类，计算N个运动员从起跑信号发出后到最后一个运动员到达终点所花费的时间
 * @author think
 *
 */
public class SprintTimer {
	private int count; //参与比赛的运动员人数
	private Runnable sprinter; //运动员
	private CountDownLatch beginLatch;
	private CountDownLatch endLatch;
	
	public SprintTimer(int count){
		this.count = count;
		beginLatch = new CountDownLatch(1);
		endLatch = new CountDownLatch(count);
	}
	
	
	public void race() throws Exception{
		ExecutorService exec = Executors.newFixedThreadPool(count);
		for(int i = 0; i < count; i++){
			sprinter = new Sprinter(i+"号",beginLatch,endLatch);
			exec.execute(sprinter);
		}
		DateFormat df = new SimpleDateFormat("HH:mm:ss :SS");
		String date = df.format(new Date(System.currentTimeMillis()));
		System.out.println("各就各位, 预备，跑!!!"+date);
		beginLatch.countDown();
		long beginTime = System.currentTimeMillis();
		endLatch.await();
		long endTime = System.currentTimeMillis();
		System.out.println(count+"个运动员跑完比赛共耗时"+(endTime - beginTime) / 1000.0+"秒");
	}
	
	public static void main(String[] args) {
		SprintTimer sprintTimer = new SprintTimer(8);
		try {
			sprintTimer.race();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String getTime(long mills){
		DateFormat df = new SimpleDateFormat("HH:mm:ss :SS");
		String date = df.format(new Date(mills));
		return date;
	}

}


/**
 * 运动员
 * @author think
 *
 */
class  Sprinter implements Runnable{
	
	private String sprinterName;
	private CountDownLatch beginLatch;
	private CountDownLatch endLatch;
	
	public Sprinter(String sprinterName,CountDownLatch beginLatch,CountDownLatch endLatch){
		this.sprinterName = sprinterName;
		this.beginLatch = beginLatch;
		this.endLatch = endLatch;
	}
	
	@Override
	public void run(){
		try {
			beginLatch.await(); //等待开始信号
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}	
		Random random  = new Random();
		int millSecond = random.nextInt(3000) + 9000;//运动员跑完100米需要时间从9秒到12秒
		long begin = System.currentTimeMillis();
		System.out.println("运动员["+sprinterName+"]开始起跑,时间点:"+SprintTimer.getTime(begin));
		try {
			Thread.sleep(millSecond);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		System.out.println("运动员["+sprinterName+"]冲过终点,时间:"+SprintTimer.getTime(end)+",耗时"+(end-begin) /1000.0 +"秒");
		endLatch.countDown(); //表示已经到达终点
	}
}