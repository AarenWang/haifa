package me.wrj.concurrent.reactive;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates manual backpressure control by requesting data in fixed size
 * batches. A slow subscriber forces the publisher to respect its pace.
 */
public class ReactiveBackpressureDemo {

    public static void main(String[] args) throws Exception {
        try (SubmissionPublisher<Integer> publisher = new SubmissionPublisher<>()) {
            publisher.subscribe(new SlowSubscriber(3));

            for (int i = 1; i <= 10; i++) {
                System.out.printf("publishing %d%n", i);
                publisher.submit(i);
            }
        }

        Thread.sleep(1000);
    }

    static class SlowSubscriber implements Flow.Subscriber<Integer> {
        private static final int BATCH_SIZE = 2;

        private final AtomicInteger received = new AtomicInteger();
        private final int delaySeconds;
        private Flow.Subscription subscription;

        SlowSubscriber(int delaySeconds) {
            this.delaySeconds = delaySeconds;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            requestMore();
        }

        @Override
        public void onNext(Integer item) {
            System.out.printf("slow subscriber received %d on %s%n", item, Thread.currentThread().getName());
            received.incrementAndGet();
            sleep();
            if (received.get() % BATCH_SIZE == 0) {
                requestMore();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            System.err.println("slow subscriber error: " + throwable.getMessage());
        }

        @Override
        public void onComplete() {
            System.out.println("slow subscriber complete");
        }

        private void requestMore() {
            subscription.request(BATCH_SIZE);
        }

        private void sleep() {
            try {
                Thread.sleep(delaySeconds * 100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
