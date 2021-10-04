package org.wrj.sync.notifywait;

import java.util.Random;

public class Product implements Runnable {

	private Container container;

	private Random random = new Random();

	private static int totalProductCount = 0;

	private int currentProductCount = 0;

	public Product(Container container) {
		this.container = container;
	}

	@Override
	public void run() {
		while (true) {
			synchronized (container) {
				if (container.getProductPosition() >= container.getCapacity()) {
					try {
						System.out.println("容器已满，生产者线程"
								+ Thread.currentThread().getName() + "进入等待");
						container.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				container.productGood(new Object());
				totalProductCount++;
				currentProductCount++;
				System.out.println("生产者线程" + Thread.currentThread().getName()
						+ "生产产品" + container.getConsumerPosition()+",共生产产品"+totalProductCount+"个，当前线程生产"+currentProductCount+"个");
				container.notifyAll();
			}

			try {
				Thread.sleep(random.nextInt(1000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
