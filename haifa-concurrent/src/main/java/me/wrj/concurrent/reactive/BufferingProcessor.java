package me.wrj.concurrent.reactive;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * Collects incoming elements into a fixed-size buffer before publishing them as
 * batches downstream. This allows downstream subscribers to process data in
 * groups, which can be used for bulk I/O operations.
 */
class BufferingProcessor<T> extends SubmissionPublisher<List<T>> implements Flow.Processor<T, List<T>> {

    private final int batchSize;
    private Flow.Subscription subscription;
    private final List<T> buffer;

    BufferingProcessor(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        this.batchSize = batchSize;
        this.buffer = new ArrayList<>(batchSize);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(batchSize);
    }

    @Override
    public void onNext(T item) {
        buffer.add(item);
        if (buffer.size() == batchSize) {
            submit(flush());
            subscription.request(batchSize);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        closeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        if (!buffer.isEmpty()) {
            submit(flush());
        }
        close();
    }

    private List<T> flush() {
        List<T> copy = new ArrayList<>(buffer);
        buffer.clear();
        return copy;
    }
}
