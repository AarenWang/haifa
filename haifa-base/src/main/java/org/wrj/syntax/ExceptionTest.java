package org.wrj.syntax;

public class ExceptionTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ExceptionTest test = new ExceptionTest();
		test.method1();
		test.method2();
		test.method3();

	}
	
	private void method1(){
		System.out.println("method1");
	}
	
	private void method2(){
		System.out.println("begnin method2");
		throw new RuntimeException("method2 trow exception");
		//System.out.println("end method2");
	}
	
	private void method3(){
		System.out.println("method3");
	}

}
