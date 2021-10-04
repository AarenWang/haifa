package org.wrj.sync.condition;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProductConsumerTest {

	public static void main(String[] args) {
		Container container = new Container(10);
		Lock lock = new ReentrantLock();
		Condition productCondition = lock.newCondition();
		Condition consumerCondition = lock.newCondition();
		Product p1 = new Product(container, lock, productCondition,
				consumerCondition);
		Product p2 = new Product(container, lock, productCondition,
				consumerCondition);
		/*Product p3 = new Product(container, lock, productCondition,
				consumerCondition);*/

		Consumer c1 = new Consumer(container, lock, productCondition,
				consumerCondition);
		Consumer c2 = new Consumer(container, lock, productCondition,
				consumerCondition);
		Consumer c3 = new Consumer(container, lock, productCondition,
				consumerCondition);
		Consumer c4 = new Consumer(container, lock, productCondition,
				consumerCondition);
		

		new Thread(p1).start();
		new Thread(p2).start();
//		new Thread(p3).start();

		new Thread(c1).start();
		new Thread(c2).start();
		new Thread(c3).start();
		new Thread(c4).start();

	}

}
