package org.wrj.sync.condition;

import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Product implements Runnable {

	private Container container;

	private Random random = new Random();

	private Lock lock;

	private Condition productCondition;

	private Condition consumeCondition;
	
    private static int totalProductCount = 0;
	
	private  int currentProductCount = 0;
	

	public Product(Container container, Lock lock, Condition productCondition,
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
				if (container.getProductPosition() >= container.getCapacity()) {
					System.out.println("容器已满，生产者线程"
							+ Thread.currentThread().getName() + "进入等待");
					productCondition.await();
				}

				container.productGood(new Object());
				totalProductCount++;
				currentProductCount++;
				System.out.println("生产者线程" + Thread.currentThread().getName()
						+ "生产产品" + (container.getConsumerPosotion()) +",共生产"+totalProductCount+"个产品,当前线程生产"+currentProductCount+"个产品");
				consumeCondition.signal();

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				lock.unlock();
			}

			try {
				Thread.sleep(random.nextInt(1000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
