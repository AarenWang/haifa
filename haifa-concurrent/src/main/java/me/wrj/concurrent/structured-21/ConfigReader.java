package me.wrj.concurrent.structured21;

import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;

/**
 * First-successful-result-wins example using ShutdownOnSuccess.
 * Compile and run with JDK 21 and --enable-preview.
 */
public final class ConfigReader {

    public String readFeatureFlag() throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            scope.fork(() -> readFromPrimary("new-checkout"));
            scope.fork(() -> readFromBackup("new-checkout"));

            scope.join();
            return scope.result();
        }
    }

    private String readFromPrimary(String key) throws InterruptedException {
        pause(220);
        return key + "=enabled (primary)";
    }

    private String readFromBackup(String key) throws InterruptedException {
        pause(80);
        return key + "=enabled (backup)";
    }

    private static void pause(long millis) throws InterruptedException {
        Thread.sleep(Duration.ofMillis(millis));
    }

    public static void main(String[] args) throws Exception {
        System.out.println(new ConfigReader().readFeatureFlag());
    }
}
