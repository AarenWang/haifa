package org.wrj.spring.ai.ecommerce.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductMatchResponse(
        String id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        List<String> tags,
        double score
) {
}
