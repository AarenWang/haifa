package me.wrj.concurrent.executor;

import java.time.LocalDateTime;
import java.util.concurrent.*;

public class RejectTest   {


    public static void main(String[] args) throws InterruptedException {
        // --- 线程池配置 ---
        int corePoolSize = 2;       // 核心线程数
        int maximumPoolSize = 2;    // 最大线程数 (与核心数相同，创建一个固定大小的线程池)
        long keepAliveTime = 1L;    // 非核心线程空闲存活时间
        TimeUnit unit = TimeUnit.MINUTES;
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(5); // 容量为5的工作队列

        // 创建自定义的线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                new BlockWhenQueueFullPolicy() // 使用我们自定义的阻塞拒绝策略
        );

        System.out.println("开始向线程池提交任务...");

        // 提交10个任务
        // 2个任务会立即被线程执行
        // 5个任务会进入队列
        // 第8个任务提交时，队列已满，提交线程(main)将会被阻塞
        for (int i = 1; i <= 10; i++) {
            System.out.println("正在提交任务 " + i + "...");
            executor.submit(new Task(i));
            System.out.println("任务 " + i + " 提交成功。当前队列大小: " + executor.getQueue().size());
        }

        System.out.println("所有任务都已提交完毕。");

        // 关闭线程池
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);

        System.out.println("线程池已关闭，程序结束。");

    }

    /** 当队列已满时阻塞调用线程，直到有空位再把任务放进去 */
    public static class BlockWhenQueueFullPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            // 获取线程池的工作队列
            BlockingQueue<Runnable> queue = executor.getQueue();
            try {
                // 调用队列的 put 方法，如果队列满了，这个方法会阻塞
                System.out.println("队列已满，任务 " + r.getClass().getName() + " 正在等待... " +
                        "提交线程: " + Thread.currentThread().getName());
                queue.put(r);
            } catch (InterruptedException e) {
                // 在等待过程中，如果线程被中断，则恢复中断状态
                Thread.currentThread().interrupt();
                // 可以选择抛出异常或记录日志
                System.err.println("任务 " + r.toString() + " 在等待入队时被中断。");
            }
        }
    }


    /**
     * 简单的任务类，用于演示
     */
    static class Task implements Runnable {
        private final int taskId;

        public Task(int taskId) {
            this.taskId = taskId;
        }

        public int getTaskId() {
            return taskId;
        }

        @Override
        public void run() {
            System.out.println(">>> 正在执行任务 " + taskId + "，执行线程: " + Thread.currentThread().getName());
            try {
                // 模拟任务执行耗时
                Thread.sleep(ThreadLocalRandom.current().nextInt(3000, 5000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("<<< 任务 " + taskId + " 执行完毕。");
        }

        @Override
        public String toString() {
            return "Task[" + taskId + "]";
        }
    }





}
