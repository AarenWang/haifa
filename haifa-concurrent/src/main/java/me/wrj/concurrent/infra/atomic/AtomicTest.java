package me.wrj.concurrent.infra.atomic;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicTest {

	public static final long count = 100000000;

	public static final long threadCount = 10;

	static void incrementInteger1() {

		AtomicInteger aInteger = new AtomicInteger(0);

		long begin = System.currentTimeMillis();
		int value = 0;
		for (int i = 0; i < count; i++) {
			value = aInteger.incrementAndGet();
		}

		long end = System.currentTimeMillis();
		System.out.println("incrementInteger1 cost " + (end - begin) + " millseconds");

	}

	static void incrementInteger2() {

		int aInteger = 0;

		long begin = System.currentTimeMillis();
		int value = 0;
		for (int i = 0; i < count; i++) {
			synchronized (AtomicTest.class) {
				value = aInteger++;
			}
		}

		long end = System.currentTimeMillis();
		System.out.println("incrementInteger2 cost " + (end - begin) + " millseconds");

	}

	public static void main(String[] args) {
		incrementInteger1();
		incrementInteger2();
		
		
		
	}

}


