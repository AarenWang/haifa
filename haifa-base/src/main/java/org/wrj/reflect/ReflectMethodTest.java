package org.wrj.reflect;

import java.lang.reflect.Method;
import java.util.Date;


public class ReflectMethodTest {
	
	public static void main(String[] args) {
		try {
			Class<Date> clazz = (Class<Date>) Class.forName("java.util.Date");
			Method[] methods = clazz.getMethods();
			for(java.lang.reflect.Method m : methods){
				System.out.println(m.getName());
			}
			
			Class<?> anyClass =  clazz.getSuperclass();
			System.out.println(anyClass.getCanonicalName());
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
