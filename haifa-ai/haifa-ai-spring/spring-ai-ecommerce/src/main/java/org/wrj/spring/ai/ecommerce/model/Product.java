package org.wrj.spring.ai.ecommerce.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record Product(
        String id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        List<String> tags) {

    public Product {
        Objects.requireNonNull(id, "Product id must not be null");
        Objects.requireNonNull(sku, "Product sku must not be null");
        Objects.requireNonNull(name, "Product name must not be null");
        Objects.requireNonNull(description, "Product description must not be null");
        Objects.requireNonNull(price, "Product price must not be null");
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public String searchableText() {
        String tagText = String.join(" ", tags);
        return String.join("\n", name, description, tagText, "SKU:" + sku, "PRICE:" + price);
    }
}
