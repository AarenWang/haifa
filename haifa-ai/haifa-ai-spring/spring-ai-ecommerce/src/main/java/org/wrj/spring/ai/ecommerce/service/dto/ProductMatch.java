package org.wrj.spring.ai.ecommerce.service.dto;

import org.wrj.spring.ai.ecommerce.model.Product;

public record ProductMatch(Product product, double score) {
}
