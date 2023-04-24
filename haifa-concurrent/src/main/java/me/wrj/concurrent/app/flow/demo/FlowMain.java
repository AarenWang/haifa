package me.wrj.concurrent.app.flow.demo;

import java.util.concurrent.*;
import java.util.function.Consumer;

public class FlowMain {

    static class SampleSubscriber<T> implements Flow.Subscriber<T> {
        final Consumer<? super T> consumer;
        Flow.Subscription subscription;
        SampleSubscriber(Consumer<? super T> consumer) {
            this.consumer = consumer;
        }
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            System.out.println("建立订阅关系");
            this.subscription = subscription; // 赋值
            subscription.request(1);
        }
        public void onNext(T item) {
            try {
                System.out.println("thread name 0 "+Thread.currentThread().getName());
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("收到发送者的消息:"+ item);
            consumer.accept(item);
            subscription.request(1);
        }
        public void onError(Throwable ex) { ex.printStackTrace(); }

        //调用了  publisher.close()，才会收到Complete消息
        public void onComplete() {
            System.out.println("订阅消费完成");
        }
    }

    public static void main(String[] args) {
        SampleSubscriber subscriber = new SampleSubscriber<>((Event e) ->{
            System.out.println("消费消息,title: 【"+e.getTitle()+"】");
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        SubmissionPublisher<Event> submissionPublisher = new SubmissionPublisher(executor,Flow.defaultBufferSize());
        submissionPublisher.subscribe(subscriber);
        for (int i = 0; i < 10; i++) {
            System.out.println("开始发布第"+i+"条消息");
            submissionPublisher.submit(new Event("event + "+ i + " occured"));
            System.out.println("第"+i+"条消息发布完毕");
        }

        submissionPublisher.close();
        executor.shutdown();

    }

    public static class Event{
        private String title;

        public Event(String title){
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return "Event{" +
                    "title='" + title + '\'' +
                    '}';
        }
    }
}
