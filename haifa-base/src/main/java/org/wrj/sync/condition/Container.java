package org.wrj.sync.condition;

public class Container {

	private Object[] container = null;

	private int productPosition = 0;
	
	private int consumerPosotion = -1;

	private int capacity;

	public Container(int capacity) {
		container = new Object[capacity];
		this.capacity = capacity;
	}

	public void productGood(Object object) {
		container[productPosition] = object;
		productPosition++;
		consumerPosotion++;
	}

	public void consumeGood() {
		container[consumerPosotion] = null;
		productPosition--;
		consumerPosotion--;
	}


	public int getConsumerPosotion() {
		return consumerPosotion;
	}
	

	public int getProductPosition() {
		return productPosition;
	}

	public int getCapacity() {
		return capacity;
	}

}
