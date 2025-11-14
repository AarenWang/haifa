package org.wrj.spring.ai.ecommerce.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.wrj.spring.ai.ecommerce.model.Product;

class ProductDocumentMapperTest {

    private final ProductDocumentMapper mapper = new ProductDocumentMapper();

    @Test
    void shouldRoundTripProduct() {
        Product product = new Product("p-1", "SKU-1", "智能音箱",
                "支持语音助手与多房间联动", new BigDecimal("399.00"), List.of("智能家居", "音频"));

        Document document = mapper.toDocument(product);
        Product restored = mapper.fromDocument(document);

        assertThat(restored.id()).isEqualTo(product.id());
        assertThat(restored.sku()).isEqualTo(product.sku());
        assertThat(restored.name()).isEqualTo(product.name());
        assertThat(restored.description()).contains("语音助手");
        assertThat(restored.price()).isEqualByComparingTo(product.price());
        assertThat(restored.tags()).containsExactlyElementsOf(product.tags());
        assertThat(document.getId()).isEqualTo(product.id());
        assertThat(document.getText()).contains(product.name());
    }
}
