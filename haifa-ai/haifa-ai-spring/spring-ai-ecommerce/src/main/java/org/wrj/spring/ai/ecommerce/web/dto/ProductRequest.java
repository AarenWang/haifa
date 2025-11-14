package org.wrj.spring.ai.ecommerce.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotBlank String id,
        @NotBlank String sku,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull @Positive BigDecimal price,
        List<String> tags
) {
}
