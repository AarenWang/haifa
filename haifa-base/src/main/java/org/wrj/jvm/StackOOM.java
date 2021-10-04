package org.wrj.jvm;

public class StackOOM {
	static int count = 0;
	public static int increment(int i){
		count++;
		return increment(i++);
	}
	
	public static void main(String[] args) {
		int begin = 0;
		try {
			increment(begin);
		} catch (Exception e) {
			
		}finally{
			System.out.println(count);
		}
	}
}
