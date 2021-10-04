package org.wrj.generic;

public class MyClass<E> {
	
	public  void  someMethod(Object item){
		 //if(item instanceof E){  //  	
          // ���������ʾ:  Cannot perform instanceof check against type parameter E. Use instead its erasure Object 
		  // instead since further generic type information will be erased at runtime

		// }
		//  E item2 = new E();  //���������ʾ: Cannot instantiate the type E	

	}
	
	public String sayHello(String name){
		System.out.println("Hello " + name);
		return "Hello " + name;
	}
	
	public static String echo(String name){
		System.out.println("Hello " + name);
		return "Hello " + name;
	}

}
