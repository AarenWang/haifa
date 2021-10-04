package org.wrj.concurrency.sync;


import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class CycliBarrierApplication
{
	private int threadCount;
	private CyclicBarrier barrier;
	private int loopCount = 20;

	public CycliBarrierApplication ( int threadCount )
	{
		this.threadCount = threadCount;
		barrier = new CyclicBarrier(threadCount, new Runnable()
		{
			public void run()
			{
				collectTestResult();
			}
		});
		for ( int i = 0; i < threadCount; ++i)
		{
			Thread thread = new Thread("test-thread " + i)
			{
				public void run()
				{
					for ( int j = 0; j < loopCount; ++j)
					{
						doTest(j);
						try
						{
							barrier.await();// ʹ��Barrier��ʵ�ֲ������ܲ��Եľۺϵ㡣
						}
						catch (InterruptedException e)
						{
							return;
						}
						catch (BrokenBarrierException e)
						{
							return;
						}
					}
				}
			};
			thread.start();
		}
	}

	private void doTest(int j )
	{ /* do xxx */
		System.out.println("Enter into the Barrier : j = "+j);
	}

	private void collectTestResult()
	{ /* do xxx */
		System.out.println("Exit the Barrier: ");
	}
	public static void main(String[] args)
	{
		CycliBarrierApplication barrier=new CycliBarrierApplication(4);
	}
}
