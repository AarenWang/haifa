package me.wrj.concurrent.app.sychronizeder.waitnotify;


public class Consumer implements Runnable{


	private Container<Integer> container;
	
	
	@Override
	public void run() {
		for(;;){
			synchronized (container) {
				if(container.isEmpty()){
					try {
						container.wait();
					} catch (InterruptedException e) {
					
						e.printStackTrace();
					}
				}
				Integer product = container.get();
				System.out.println("消费者线程"+Thread.currentThread().getName()+"消费了产品"+product+"当前容器剩余产品数量"+container.getCount());
				try {
					Thread.sleep(800);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				container.notifyAll();
			}
		}
		
		
	}
	
	
	public Consumer(Container<Integer> container) {
		this.container = container;
	}

}
