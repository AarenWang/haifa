package me.wrj.concurrent.app.sychronizeder.waitnotify;

import java.util.Random;


public class Producer implements Runnable{

	private Container<Integer> container;
	
	private Random random = new Random(100000);
	
	@Override
	public void run() {
	    for(;;){
	    	synchronized (container) {
				if(container.isFull()){
					System.out.println("容器已经满，等待消费者消费，当前线程:"+Thread.currentThread().getName());
					try {
						container.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Integer product = random.nextInt(100000);
				container.put(product);
				System.out.println("生产者线程"+Thread.currentThread().getName()+"生产出产品"+product+"放入容器，当前容器产品数量"+container.getCount());
				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				container.notifyAll();
			}
	    }
		
	}
	
	
	public Producer(Container<Integer> container){
		this.container = container;
	}

	
	

	
}
