package org.wrj.sync.notifywait;

public class ProductConsumerTest {
	
	public static void main(String[] args) {
		Container container =new Container(10);
		Product p1 = new Product(container);
		Product p2 = new Product(container);
		Product p3 = new Product(container);
		
		Consumer c1 = new Consumer(container);
		Consumer c2 = new Consumer(container);
		
		new Thread(p1).start();
		new Thread(p2).start();
		new Thread(p3).start();
		
		new Thread(c1).start();
		new Thread(c2).start();
		
	}

}
