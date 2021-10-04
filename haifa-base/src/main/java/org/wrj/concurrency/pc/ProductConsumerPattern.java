package org.wrj.concurrency.pc;

import java.util.Random;

/**
 * 使用Object的wait和notify协调线程的生产者消费者模式
 * 该生产者消费者模式存在问题有
 * 1 如果仓库满了通知后，生产者线程还是会也竞争锁
 * 2 同步代码块太大，导致并发程度不高
 * 
 * @author wangrj
 * 
 */
public class ProductConsumerPattern {
	public static void main(String[] args) {
		final int CAPACITY = 10;
		Warehouse hourse = new Warehouse(CAPACITY);
		for (int j = 0; j < 5; j++) {
			new Consumer(hourse).start();
		}
		for (int i = 0; i < 15; i++) {
			new Producter(hourse,CAPACITY).start();
		}

	}

}

class Producter extends Thread {
	private Warehouse hourse;
	private int capacity;

	public Producter(Warehouse hourse,int capacity) {
		super();
		this.hourse = hourse;
		this.capacity = capacity;
	}

	@Override
	public void run() {
		while (true) {
			synchronized (hourse) {
				while (hourse.getIndex() == capacity - 1) {
					try {
						System.out.println("仓库已经满,生产者" + this.getName()
								+ "暂停生产");
						hourse.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				String p = "p" + (hourse.getIndex() + 1);
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

class Consumer extends Thread {

	private Warehouse hourse;

	public Consumer(Warehouse hourse) {
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

class Warehouse {
	private Object[] buffer;
	private int index = -1;
	private int capacity;

	public Warehouse(int capcity) {
		buffer = new Object[capcity];
		this.capacity = capcity;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public void addProduct(String product) {
		index++;
		buffer[index] = product;
	}

	public String consumrProduct() {
		String p = (String) buffer[index];
		buffer[index] = null;
		index--;
		return p;
	}

}
