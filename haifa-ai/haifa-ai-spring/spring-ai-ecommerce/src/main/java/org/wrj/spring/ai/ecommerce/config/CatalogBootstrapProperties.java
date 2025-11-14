package org.wrj.spring.ai.ecommerce.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "catalog.bootstrap")
public record CatalogBootstrapProperties(
        boolean enabled,
        String dataset,
        Duration delay
) {
    public CatalogBootstrapProperties(@DefaultValue("true") boolean enabled,
            @DefaultValue("classpath:catalog/products.json") String dataset,
            @DefaultValue("PT0S") Duration delay) {
        this.enabled = enabled;
        this.dataset = dataset;
        this.delay = delay;
    }
}
