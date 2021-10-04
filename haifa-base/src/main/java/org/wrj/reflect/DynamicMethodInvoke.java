package org.wrj.reflect;

import java.lang.reflect.Method;

public class DynamicMethodInvoke {

	
	public static void main(String[] args) {
		try {
			Class c = Class.forName("org.wrj.reflect.Foo");
			Object obj = c.newInstance();
			Method m1 = c.getMethod("sayGreet", String.class);
			Object result1 = m1.invoke(obj, "Jack");
			System.out.println(result1);
			
			Method m2 = c.getMethod("sayGreet");
			Object result2 = m2.invoke(obj);
			System.out.println(result2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class Foo{
	
	public String sayGreet(String name){
		return "Hello " + name +" ,Welcome you";
	}
	
	public String sayGreet(){
		return "Hello everyone ,Welcome you";
	} 
	
}
