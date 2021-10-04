package me.wrj.concurrent.app.lock;

import java.time.LocalDateTime;
import java.util.concurrent.locks.LockSupport;

public class LockSupportTest {
	
	public static void main(String[] args) {
		
		System.out.println(LocalDateTime.now());
		LockSupport.park(1000L);
		System.out.println(LocalDateTime.now());

		
		LockSupport.unpark(Thread.currentThread());
		System.out.println(LocalDateTime.now());

		
		
	}

}
