package org.wrj.concurrency.semaphore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class SemaphoreDemo {
	final static int SIZE = 25;
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			final Pool<Fat> pool = new Pool<Fat>(Fat.class,SIZE);
			ExecutorService exec = Executors.newCachedThreadPool();
			for(int i = 0; i < SIZE; i++){
				exec.execute(new CheckOutTask<Fat>(pool));
			}
			System.out.println("All CheckoutTask created!");
			
			List<Fat> list = new ArrayList<Fat>();
			for(int i = 0; i < SIZE; i++){
				Fat f = pool.checkOut();
				System.out.println(i + " main() thread checked out ");
				f.operation();
				list.add(f);
			}
			
			Future<?> blocked = exec.submit(new Runnable() {
				
				@Override
				public void run() {
					try {
						pool.checkOut();
					} catch (InterruptedException e) {
						System.out.println("checkout interrupted");
					}
				}
			});
			TimeUnit.SECONDS.sleep(2);
			blocked.cancel(true);
			System.out.println("Checking in objects in " + list);
			for(Fat f:list){
				pool.checkIn(f);
			}
			for(Fat f:list){
				pool.checkIn(f);
			}
			exec.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}


class CheckOutTask<T> implements Runnable{
	private int counter = 0;
	private final int id = counter++;
	private Pool<T> pool;
	
	public CheckOutTask(Pool<T> pool){
		this.pool = pool;
	}

	@Override
	public void run() {
		try {
			T item = pool.checkOut();
			System.out.println(this + " checked out " + item);
			TimeUnit.SECONDS.sleep(1);
			System.out.println(this + " checked in " + item);
			pool.checkIn(item);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	public String toString(){
		return "CheckoutTask " + id + " ";
	}
	
}