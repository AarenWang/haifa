package org.wrj.spring.ai.ecommerce.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.wrj.spring.ai.ecommerce.model.Product;
import org.wrj.spring.ai.ecommerce.service.ProductCatalogService;

@Configuration
@EnableConfigurationProperties(CatalogBootstrapProperties.class)
public class CatalogBootstrapConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CatalogBootstrapConfiguration.class);

    @Bean
    public TaskScheduler catalogTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("catalog-bootstrap-");
        scheduler.setPoolSize(1);
        return scheduler;
    }

    @Bean
    public CatalogDatasetLoader catalogDatasetLoader(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        return new CatalogDatasetLoader(objectMapper, resourceLoader);
    }

    @Bean
    public CatalogInitializer catalogInitializer(ProductCatalogService productCatalogService,
            CatalogBootstrapProperties properties,
            CatalogDatasetLoader datasetLoader,
            TaskScheduler catalogTaskScheduler) {
        return new CatalogInitializer(productCatalogService, properties, datasetLoader, catalogTaskScheduler);
    }

    public static class CatalogDatasetLoader {
        private final ObjectMapper objectMapper;
        private final ResourceLoader resourceLoader;

        public CatalogDatasetLoader(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
            this.objectMapper = objectMapper;
            this.resourceLoader = resourceLoader;
        }

        public List<Product> load(String location) {
            try {
                Resource resource = resourceLoader.getResource(location);
                try (InputStream inputStream = resource.getInputStream()) {
                    return objectMapper.readValue(inputStream, new TypeReference<List<Product>>() {
                    });
                }
            }
            catch (IOException exception) {
                throw new IllegalStateException("Failed to load catalog dataset from " + location, exception);
            }
        }
    }

    public static class CatalogInitializer {
        private final ProductCatalogService productCatalogService;
        private final CatalogBootstrapProperties properties;
        private final CatalogDatasetLoader datasetLoader;
        private final TaskScheduler scheduler;

        public CatalogInitializer(ProductCatalogService productCatalogService,
                CatalogBootstrapProperties properties,
                CatalogDatasetLoader datasetLoader,
                TaskScheduler scheduler) {
            this.productCatalogService = productCatalogService;
            this.properties = properties;
            this.datasetLoader = datasetLoader;
            this.scheduler = scheduler;
            scheduleBootstrap();
        }

        private void scheduleBootstrap() {
            if (!properties.enabled()) {
                logger.info("Catalog bootstrap disabled");
                return;
            }
            scheduler.schedule(this::bootstrapCatalog,
                    java.util.Date.from(java.time.Instant.now().plus(properties.delay())));
        }

        private void bootstrapCatalog() {
            try {
                List<Product> products = datasetLoader.load(properties.dataset());
                productCatalogService.ingestProducts(products);
                logger.info("Loaded {} products into pgvector store", products.size());
            }
            catch (Exception exception) {
                logger.error("Failed to bootstrap catalog", exception);
            }
        }
    }
}
