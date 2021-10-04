package org.wrj.concurrency.deadlock;

class Resource {
	 
    public static Object o1 = new Object();
    public static Object o2 = new Object();
 
}

class DeadThread1 implements Runnable {
	 
    @Override
    public void run() {
        fun();
    }
 
    //fun()方法首先占用o1资源,然后休眠1秒,让给其他线程执行。
    //然后请求o2资源
    public void fun() {
        synchronized (Resource.o1) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            synchronized (Resource.o2) {
                System.out.println("DeadThread1里的fun()被执行");
            }
        }
    }
 
}

class DeadThread2 implements Runnable {
	 
    @Override
    public void run() {
        fun();
    }
     
    //fun()方法首先占用o2资源,然后休眠1秒,让给其他线程执行。
    //然后请求o1资源
    public void fun() {
        synchronized (Resource.o2) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
 
            synchronized (Resource.o1) {
                System.out.println("DeadThread1里的fun()被执行");
            }
        }
    }
 
}

public class Client {

	 public static void main(String[] args) {
	        DeadThread1 dt1 = new DeadThread1();
	        DeadThread2 dt2 = new DeadThread2();
	         
	        Thread t1 = new Thread(dt1);
	        t1.setName("Thread1");
	        Thread t2 = new Thread(dt2);
	        t1.setName("Thread2");
	         
	        //启动两个线程
	        t1.start();
	        t2.start();
	         
	    }   
}
