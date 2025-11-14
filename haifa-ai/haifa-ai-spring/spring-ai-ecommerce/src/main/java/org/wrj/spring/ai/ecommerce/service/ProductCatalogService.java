package org.wrj.spring.ai.ecommerce.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.wrj.spring.ai.ecommerce.ingest.ProductDocumentMapper;
import org.wrj.spring.ai.ecommerce.model.Product;
import org.wrj.spring.ai.ecommerce.service.dto.ProductMatch;

@Service
public class ProductCatalogService {

    private final VectorStore vectorStore;
    private final ProductDocumentMapper documentMapper;
    private final Map<String, Product> catalog = new ConcurrentHashMap<>();

    public ProductCatalogService(VectorStore vectorStore, ProductDocumentMapper documentMapper) {
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");
        this.documentMapper = Objects.requireNonNull(documentMapper, "documentMapper must not be null");
    }

    public void ingestProducts(List<Product> products) {
        Assert.notEmpty(products, "products must not be empty");
        List<Document> documents = new ArrayList<>(products.size());
        for (Product product : products) {
            catalog.put(product.id(), product);
            documents.add(documentMapper.toDocument(product));
        }
        vectorStore.add(documents);
    }

    public List<ProductMatch> search(String query, int topK) {
        Assert.hasText(query, "query must not be blank");
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        return toMatches(vectorStore.similaritySearch(request));
    }

    public List<ProductMatch> recommendSimilar(String productId, int topK) {
        Product product = Optional.ofNullable(catalog.get(productId))
                .orElseThrow(() -> new IllegalArgumentException("Unknown product id: " + productId));
        List<ProductMatch> matches = search(product.searchableText(), topK + 1);
        return matches.stream()
                .filter(match -> !match.product().id().equals(productId))
                .limit(topK)
                .collect(Collectors.toList());
    }

    public Optional<Product> findById(String productId) {
        return Optional.ofNullable(catalog.get(productId));
    }

    private List<ProductMatch> toMatches(List<Document> documents) {
        return documents.stream()
                .map(document -> new ProductMatch(documentMapper.fromDocument(document),
                        document.getScore() == null ? 0.0 : document.getScore()))
                .collect(Collectors.toList());
    }
}
