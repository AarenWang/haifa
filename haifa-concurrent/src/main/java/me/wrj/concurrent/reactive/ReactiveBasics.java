package me.wrj.concurrent.reactive;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * A minimal example demonstrating how the Java 9 Flow API can be used to build
 * a reactive pipeline. The publisher pushes a small list of messages to a
 * subscriber that prints each element on the calling thread.
 */
public class ReactiveBasics {

    public static void main(String[] args) throws Exception {
        try (SubmissionPublisher<String> publisher = new SubmissionPublisher<>()) {
            Flow.Subscriber<String> subscriber = new LoggingSubscriber("basic-subscriber");
            publisher.subscribe(subscriber);

            List.of("hello", "reactive", "world").forEach(publisher::submit);
        }

        // wait briefly to ensure the async subscriber has time to process
        Thread.sleep(200);
    }

    /**
     * A tiny subscriber implementation that requests all data upfront and
     * simply logs the items it receives.
     */
    static class LoggingSubscriber implements Flow.Subscriber<String> {
        private final String name;
        private Flow.Subscription subscription;

        LoggingSubscriber(String name) {
            this.name = name;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
            System.out.printf("[%s] subscribed\n", name);
        }

        @Override
        public void onNext(String item) {
            System.out.printf("[%s] received: %s\n", name, item);
        }

        @Override
        public void onError(Throwable throwable) {
            System.err.printf("[%s] error: %s\n", name, throwable.getMessage());
        }

        @Override
        public void onComplete() {
            System.out.printf("[%s] complete\n", name);
        }
    }
}
