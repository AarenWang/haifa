package org.wrj.sync.notifywait;

public class Container {

	private Object[] container = null;

	private int productPosition = 0;
	
	private int consumerPosition = -1;

	private int capacity;

	public Container(int capacity) {
		container = new Object[capacity];
		this.capacity = capacity;
	}

	public void productGood(Object object) {
		container[productPosition] = object;
		productPosition++;
	}

	public void consumeGood() {
		container[consumerPosition] = null;
		consumerPosition--;
	}


	public int getCapacity() {
		return capacity;
	}

	public int getProductPosition() {
		return productPosition;
	}

	public int getConsumerPosition() {
		return consumerPosition;
	}

	
}
