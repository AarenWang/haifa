package me.wrj.concurrent.app.sychronizeder.waitnotify;

import java.util.ArrayList;
import java.util.List;

public class Container<T> {

	private static final int DEFAULT_SIZE = 10;

	private int size = DEFAULT_SIZE;

	private List<T> list = new ArrayList<>();

	public boolean isFull() {
		return list.size() == size;
	}

	public boolean isEmpty() {
		return list.size() == 0;
	}

	public void put(T t) {
		list.add(t);
	}

	public T get() {
		T t = list.remove(list.size() - 1);
		return t;
	}
	
	
	public int getCount(){
		return list.size();
	}
	

}
