package me.wrj.concurrent.app.sychronizeder.waitnotify;

public class Client {

	
	public static void main(String[] args) {
		Container<Integer> container = new Container<Integer>();
		Producer producer = new Producer(container);
		new Thread(producer).start();
		
		Consumer consumer = new Consumer(container);
		new Thread(consumer).start();
		
		
		//多个生产中消费者需要用另外同步方法Condition
//		for(int i =0; i < 5; i++){
//			Producer producer = new Producer(container);
//			new Thread(producer).start();
//		}
//		
//		
//		for(int i =0; i < 10; i++){
//			Consumer consumer = new Consumer(container);
//			new Thread(consumer).start();
//		}
		
	}
}
