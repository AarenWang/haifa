package org.wrj.generic;

public class Box<T> {
	
	private T t;
	
	public void add(T t){
		this.t = t;
	}

	public T get(){
		return t;
	}
	
	public  <U extends Number> void inspect(U u){
		System.out.println("T: " + t.getClass().getName());
		System.out.println("U: " + u.getClass().getName());
	}
}
