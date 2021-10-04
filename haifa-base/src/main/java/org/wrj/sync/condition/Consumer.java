package org.wrj.sync.condition;

import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Consumer implements Runnable {

	private Container container;

	private Random random = new Random(2000);

	private Lock lock;

	private Condition productCondition;

	private Condition consumeCondition;
	
	private static int totalConsumerCount = 0;
	
	private  int currentConsumerCount = 0;
	

	public Consumer(Container container, Lock lock, Condition productCondition,
			Condition consumeCondition) {
		this.container = container;
		this.lock = lock;
		this.productCondition = productCondition;
		this.consumeCondition = consumeCondition;
	}

	@Override
	public void run() {
		while (true) {
			try {
				lock.tryLock();
				if (container.getConsumerPosotion() == -1) {
					System.out.println("容器已空，消费者线程"
							+ Thread.currentThread().getName() + "进入等待");
					consumeCondition.await();
				}
				container.consumeGood();
				totalConsumerCount++;
				currentConsumerCount++;
				System.out.println("消费者线程" + Thread.currentThread().getName()
						+ "消费产品" + container.getProductPosition()+",共消费"+totalConsumerCount+"个产品,当前线程消费"+currentConsumerCount+"个产品");
				productCondition.signal();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				lock.unlock();
			}

			try {
				Thread.sleep(random.nextInt(2000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
