package org.wrj.spring.ai.ecommerce.ingest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.wrj.spring.ai.ecommerce.model.Product;

@Component
public class ProductDocumentMapper {

    public Document toDocument(Product product) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", product.id());
        metadata.put("sku", product.sku());
        metadata.put("name", product.name());
        metadata.put("description", product.description());
        metadata.put("price", product.price());
        metadata.put("tags", product.tags());
        return new Document(product.id(), product.searchableText(), metadata);
    }

    public Product fromDocument(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        String id = (String) metadata.getOrDefault("id", document.getId());
        String sku = (String) metadata.getOrDefault("sku", "");
        String name = (String) metadata.getOrDefault("name", "");
        String description = (String) metadata.getOrDefault("description", document.getText());
        Object priceValue = metadata.get("price");
        BigDecimal price = toBigDecimal(priceValue);
        List<String> tags = toStringList(metadata.get("tags"));
        return new Product(id, sku, name, description, price, tags);
    }

    private BigDecimal toBigDecimal(Object priceValue) {
        if (priceValue instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (priceValue instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (priceValue instanceof String string) {
            return new BigDecimal(string);
        }
        return BigDecimal.ZERO;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> results = new ArrayList<>(list.size());
            for (Object element : list) {
                if (element != null) {
                    results.add(element.toString());
                }
            }
            return results;
        }
        if (value instanceof String string) {
            return List.of(string);
        }
        return List.of();
    }
}
