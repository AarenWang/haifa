package org.wrj.concurrency;

import java.util.concurrent.atomic.AtomicLong;

public class TestSpeed {

	private static long value1 = 0L;
	private volatile static long value2 = 0L;
	private static AtomicLong value3 = new AtomicLong(0);
	private static Long value4 = 0L;

	public static void main(String[] args) {
		long begin = System.currentTimeMillis();
		increment1();
		long step1 = System.currentTimeMillis();
		increment2();
		long step2 = System.currentTimeMillis();
		increment3();
		long step3 = System.currentTimeMillis();
		increment4();
		long step4 = System.currentTimeMillis();
		System.out.println((step1 - begin) + ":" + (step2 - step1) +":"+ (step3 -step2)+ ":" + (step4 -step3));
	}

	public static void increment1() {

		while (value1 < 100000000L) {
			value1++;
		}

	}

	public static void increment2() {

		while (value2 < 100000000L) {
			value2++;
		}
	}

	public static void increment3() {

		while (value3.get() < 100000000L) {
			value3.incrementAndGet();
		}
	}
	
	public static void increment4() {

		while (value4 < 100000000L) {
			value4++;
		}
	}

}
