package org.wrj.concurrency.atomic;

import java.util.concurrent.atomic.AtomicInteger;

public class TestAtomicInteger {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AtomicIntegerSequence ais = new AtomicIntegerSequence(0);
		IntegerSequence is = new IntegerSequence(0);
		int count = 0;
		long begin = System.currentTimeMillis();
		while(count < 1000000){
			ais.getNextValue();
			count++;
		}
		
		long step1 = System.currentTimeMillis();
		count = 0;
		while(count < 1000000){
			is.getNextValue();
			count++;
		}
		
		long step2 = System.currentTimeMillis();
		System.out.println((step2- step1) +":" + (step1 - begin));
	}
	
	

}


class AtomicIntegerSequence{
	private AtomicInteger ai;
	
	public AtomicIntegerSequence(int begin){
		ai = new AtomicInteger(begin);
	}
	
	public int getNextValue(){
		return ai.incrementAndGet();
	}	
}

class IntegerSequence{
	private int i;
	
	public IntegerSequence(int begin){
		this.i = begin;
	}
	
	public synchronized int getNextValue(){
		i = i + 1;
		return i;
	}
}
