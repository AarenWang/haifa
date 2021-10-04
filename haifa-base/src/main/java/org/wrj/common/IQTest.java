package org.wrj.common;

public class IQTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for (int i = 0;; i++) {
			switch (i % 10) {
			case 1:
				System.out.println("B=D");
				break;
			case 2:
				System.out.println("B===D");
				break;
			case 3:
				System.out.println("B=====D");
				break;
			case 4:
				System.out.println("B=======D");
				break;
			case 5:
				System.out.println("B=====D");
				break;
			case 6:
				System.out.println("B===D");
				break;
			}
		}
	}

}
