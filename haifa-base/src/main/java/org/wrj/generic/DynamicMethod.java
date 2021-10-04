package org.wrj.generic;

import java.lang.reflect.Method;

public class DynamicMethod {
	
	public static void main(String[] args) {
		try {
			Class c = Class.forName("test.generic.MyClasss");
			Class types[] = {String.class};
			Method m = c.getMethod("sayHello", types);
			MyClass my = new MyClass();
			String[] ar = {"Jack"};
			String result = (String)m.invoke(my, ar);
			System.out.println("result is:"+result);
			
			m = c.getMethod("echo", types);
			result = (String)m.invoke(my, ar);
			System.out.println("result is:"+result);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
