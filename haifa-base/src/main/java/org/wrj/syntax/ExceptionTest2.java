package org.wrj.syntax;

public class ExceptionTest2 {
	
	
	public static void main(String[] args) {
		try {
			throw new RuntimeException("throw runtimeexception!!");
		} catch (Exception e) {
			System.out.println("exception raise");
			//System.exit(0);
		} finally{
			System.out.println("exception finally");
		}
		System.out.println("out of try catch finally");
		
	}
	
	
	
	
	

}
