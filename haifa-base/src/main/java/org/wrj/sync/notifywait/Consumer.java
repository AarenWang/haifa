package org.wrj.sync.notifywait;

import java.util.Random;

public class Consumer implements Runnable {

	private Container container;

	private Random random = new Random(2000);
	
	private static int totalConsumerCount = 0;
	
	private int currentConsumerCount = 0;

	public Consumer(Container container) {
		this.container = container;
	}

	@Override
	public void run() {
		while (true) {
			synchronized (container) {
				if (container.getConsumerPosition() < 0) {
					try {
						System.out.println("容器已空，消费者线程"
								+ Thread.currentThread().getName() + "进入等待");
						container.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				container.consumeGood();
				totalConsumerCount++;
				currentConsumerCount++;
				System.out.println("消费者线程" + Thread.currentThread().getName()
						+ "消费产品" + container.getProductPosition()+",共消费"+totalConsumerCount+"个，当前线程消费"+currentConsumerCount+"个");
				container.notifyAll();

			}

			try {
				Thread.sleep(random.nextInt(2000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
