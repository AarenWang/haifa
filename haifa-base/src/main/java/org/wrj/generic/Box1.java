package org.wrj.generic;

public class Box1<T> {

	private T t;

	private void add(T t) {
		this.t = t;
	}

	public T get() {
		return t;
	}

	// Bounded Type Paramters
	public <U extends Number> void inspect(U u) {
		System.out.println("T: " + t.getClass().getName());
		System.out.println("U: " + u.getClass().getName());
	}

	public void boxTest(Box1<Number> u) {
		System.out.println(u.getClass().getName());
	}

	public static void main(String[] args) {
		Box1<Integer> integerBox = new Box1<Integer>();
		integerBox.add(new Integer(10));
		integerBox.inspect(new Float(12.3));
		// Box1<Integer> u = new Box1<Integer>(); ���Ͳ�����
		Box1<Number> u = new Box1<Number>();
		integerBox.boxTest(u);
	}
}

class Animal {

}

class Lion extends Animal {

}

class Butterfly extends Animal {

}
