package org.wrj.spring.ai.ecommerce.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.wrj.spring.ai.ecommerce.model.Product;
import org.wrj.spring.ai.ecommerce.service.ProductCatalogService;
import org.wrj.spring.ai.ecommerce.service.dto.ProductMatch;
import org.wrj.spring.ai.ecommerce.web.dto.ProductMatchResponse;
import org.wrj.spring.ai.ecommerce.web.dto.ProductRequest;

@RestController
@RequestMapping("/api/products")
@Validated
public class ProductCatalogController {

    private final ProductCatalogService productCatalogService;

    public ProductCatalogController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void ingest(@Valid @RequestBody List<ProductRequest> requests) {
        List<Product> products = requests.stream()
                .map(request -> new Product(request.id(), request.sku(), request.name(), request.description(),
                        request.price(), request.tags()))
                .collect(Collectors.toList());
        productCatalogService.ingestProducts(products);
    }

    @GetMapping("/search")
    public List<ProductMatchResponse> search(@RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "5") @Min(1) int topK) {
        return productCatalogService.search(query, topK).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}/recommendations")
    public List<ProductMatchResponse> recommend(@PathVariable("id") String productId,
            @RequestParam(defaultValue = "5") @Min(1) int topK) {
        return productCatalogService.recommendSimilar(productId, topK).stream()
                .map(this::toResponse)
                .toList();
    }

    private ProductMatchResponse toResponse(ProductMatch match) {
        Product product = match.product();
        return new ProductMatchResponse(product.id(), product.sku(), product.name(), product.description(),
                product.price(), product.tags(), match.score());
    }
}
