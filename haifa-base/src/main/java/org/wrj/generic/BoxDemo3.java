package org.wrj.generic;

import java.util.Date;


public class BoxDemo3 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Box<Integer> integerBox = new Box<Integer>();
		integerBox.add(10);
		//integerBox.add("10");  complier error
		Integer someInteger = integerBox.get();
		System.out.println(someInteger);
		
		integerBox.inspect(new Float("0.001"));

	}

}
