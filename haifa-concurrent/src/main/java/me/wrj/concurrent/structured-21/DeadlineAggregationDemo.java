package me.wrj.concurrent.structured21;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeoutException;

/**
 * Applies one deadline to the whole structured operation.
 * Compile and run with JDK 21 and --enable-preview.
 */
public final class DeadlineAggregationDemo {

    public String loadWithDeadline(Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var account = scope.fork(this::loadAccount);
            var permissions = scope.fork(this::loadPermissions);

            try {
                scope.joinUntil(deadline).throwIfFailed();
                return account.get() + ":" + permissions.get();
            } catch (TimeoutException exception) {
                throw new IllegalStateException("request timed out", exception);
            }
        }
    }

    private String loadAccount() throws InterruptedException {
        Thread.sleep(Duration.ofMillis(100));
        return "account:alice";
    }

    private String loadPermissions() throws InterruptedException {
        Thread.sleep(Duration.ofMillis(150));
        return "permissions:read";
    }

    public static void main(String[] args) throws Exception {
        System.out.println(new DeadlineAggregationDemo().loadWithDeadline(Duration.ofMillis(500)));
    }
}
