package me.wrj.concurrent.reactive;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chains multiple processors to form a small reactive data pipeline. A
 * transform processor enriches the data, and a buffering processor aggregates
 * elements before sending them to the subscriber.
 */
public class ReactiveAdvancedPipeline {

    public static void main(String[] args) throws Exception {
        try (SubmissionPublisher<String> publisher = new SubmissionPublisher<>();
             TransformProcessor<String, Event> transformer = new TransformProcessor<>(ReactiveAdvancedPipeline::toEvent);
             BufferingProcessor<Event> bufferingProcessor = new BufferingProcessor<>(3)) {

            publisher.subscribe(transformer);
            transformer.subscribe(bufferingProcessor);
            bufferingProcessor.subscribe(new LoggingSubscriber());

            List.of("alpha", "beta", "gamma", "delta", "epsilon", "zeta").forEach(publisher::submit);
        }

        Thread.sleep(500);
    }

    private static Event toEvent(String name) {
        return new Event(name, Instant.now(), ThreadLocalRandom.current().nextInt(1, 10));
    }

    record Event(String name, Instant timestamp, int weight) {
    }

    static class LoggingSubscriber implements Flow.Subscriber<List<Event>> {
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<Event> item) {
            System.out.printf("received batch (%d items) latency %s%n", item.size(),
                    Duration.between(item.get(0).timestamp(), Instant.now()));
            item.forEach(event -> System.out.printf(" -> %s%n", event));
        }

        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
        }

        @Override
        public void onComplete() {
            System.out.println("pipeline complete");
        }
    }
}
