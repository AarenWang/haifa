package org.wrj.concurrency.pc;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 使用阻塞队列实现生产者消费者模式
 * @author wangrj
 *
 */
public class ProductConsumerPattern2 {
	

}

class Producter2 extends Thread {
	private Warehouse2 hourse;
	private int capacity;

	public Producter2(Warehouse2 hourse,int capacity) {
		super();
		this.hourse = hourse;
		this.capacity = capacity;
	}

	@Override
	public void run() {
		while (true) {
			synchronized (hourse) {
				
				String p = "p" + (hourse.getIndex());
				hourse.addProduct(p);
				System.out.println("生产者线程" + this.getName() + "成功生产产品" + p+",仓库剩下空位为"+(capacity - hourse.getIndex()));
				hourse.notifyAll();
			}
			try {
				Thread.sleep(1000 + new Random().nextInt(2000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
}

class Consumer2 extends Thread {

	private Warehouse hourse;

	public Consumer2(Warehouse hourse) {
		super();
		this.hourse = hourse;
	}

	@Override
	public void run() {
		while (true) {
			synchronized (hourse) {
				while (hourse.getIndex() == -1) {
					try {
						System.out.println("仓库里没有产品，消费者等待中...");
						hourse.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				String p = hourse.consumrProduct();
				System.out.println("消费者线程" + this.getName() + "正在消费" + p+",剩余产品"+(hourse.getIndex() +1)+"个");
				hourse.notifyAll();
			}
			try {
				Thread.sleep(1000 + new Random().nextInt(2000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}

class Warehouse2{
	private BlockingQueue<String> buffer;
	private int index = 0;

	public Warehouse2(int capacity) {
		buffer = new ArrayBlockingQueue<String>(capacity);
	}


	public void addProduct(String product) {
		try {
			buffer.put(product);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getIndex() {
		return index;
	}


	public void setIndex(int index) {
		this.index = index;
	}


	public String consumrProduct() {
		String result = null;
		try {
			result =  buffer.take();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	

}