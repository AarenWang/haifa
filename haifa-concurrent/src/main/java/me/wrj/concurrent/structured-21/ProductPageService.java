package me.wrj.concurrent.structured21;

import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;

/**
 * All-or-nothing product-page aggregation using ShutdownOnFailure.
 * Compile and run with JDK 21 and --enable-preview.
 */
public final class ProductPageService {

    public record Product(String id, String name) { }
    public record Stock(String productId, int quantity) { }
    public record Price(String productId, long cents) { }
    public record ProductPage(Product product, Stock stock, Price price) { }

    public ProductPage load(String productId) throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var product = scope.fork(() -> loadProduct(productId));
            var stock = scope.fork(() -> loadStock(productId));
            var price = scope.fork(() -> loadPrice(productId));

            scope.join().throwIfFailed();
            return new ProductPage(product.get(), stock.get(), price.get());
        }
    }

    private Product loadProduct(String productId) throws InterruptedException {
        pause(120);
        return new Product(productId, "Java 21 in Action");
    }

    private Stock loadStock(String productId) throws InterruptedException {
        pause(180);
        return new Stock(productId, 42);
    }

    private Price loadPrice(String productId) throws InterruptedException {
        pause(90);
        return new Price(productId, 4_990);
    }

    private static void pause(long millis) throws InterruptedException {
        Thread.sleep(Duration.ofMillis(millis));
    }

    public static void main(String[] args) throws Exception {
        System.out.println(new ProductPageService().load("book-21"));
    }
}
