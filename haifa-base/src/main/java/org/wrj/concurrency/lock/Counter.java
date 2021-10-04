package org.wrj.concurrency.lock;

import java.util.concurrent.atomic.AtomicInteger;

public class Counter {
    /** 
	private volatile int max = 0;
	public synchronized void set(int value)
	{
		if(value > max)
		{
			max = value;
		}
	}
	
	public int getMax()
	{
		return max;
	}
	**/
	private AtomicInteger max = new AtomicInteger();
	
	public void set(int value)
	{
		for(;;)
		{
			int current = max.get();
			if(value > current)
			{
				if(max.compareAndSet(current, value))
				{
					break;
				}
				else 
				{
					continue;
				}
			}
		}
	}

}
