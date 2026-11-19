package org.wrj.haifa.ai.deerflow.research;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.wrj.haifa.ai.deerflow.persistence.entity.CompressionCacheEntity;
import org.wrj.haifa.ai.deerflow.persistence.repository.CompressionCacheRepository;

class CompressionCacheServiceTest {

    @Test
    void computesOnMissAndHitsOnSecondCall() {
        CompressionCacheRepository repository = Mockito.mock(CompressionCacheRepository.class);
        Mockito.when(repository.findByCacheKeyAndExpiresAtAfter(any(), any())).thenReturn(Optional.empty());

        CompressionCacheService service = new CompressionCacheService(repository);
        AtomicInteger computeCount = new AtomicInteger();

        String key = CompressionCacheService.computeCacheKey("v1", "generic", "default", "p1", "c1", "https://example.com", "hash123");

        String result1 = service.getOrCompute(key, "v1", "generic", "default", "p1", "https://example.com", "hash123", 100, Duration.ofDays(7), () -> {
            computeCount.incrementAndGet();
            return "Summary content";
        });

        assertThat(result1).isEqualTo("Summary content");
        assertThat(computeCount.get()).isEqualTo(1);

        // Now mock repository hit
        CompressionCacheEntity entity = new CompressionCacheEntity();
        entity.setCompressedBody("Summary content");
        entity.setHitCount(0);
        Mockito.when(repository.findByCacheKeyAndExpiresAtAfter(any(), any())).thenReturn(Optional.of(entity));

        String result2 = service.getOrCompute(key, "v1", "generic", "default", "p1", "https://example.com", "hash123", 100, Duration.ofDays(7), () -> {
            computeCount.incrementAndGet();
            return "Summary content";
        });

        assertThat(result2).isEqualTo("Summary content");
        // Compute count remains 1 because second call was a cache HIT
        assertThat(computeCount.get()).isEqualTo(1);
    }

    @Test
    void doesNotRecomputeWhenPersistenceFails() {
        CompressionCacheRepository repository = Mockito.mock(CompressionCacheRepository.class);
        Mockito.when(repository.findByCacheKeyAndExpiresAtAfter(any(), any())).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("database unavailable")).when(repository).save(any());

        CompressionCacheService service = new CompressionCacheService(repository);
        AtomicInteger computeCount = new AtomicInteger();
        String result = service.getOrCompute("cache-key", "v1", "generic", "default", "p1",
                "https://example.com", "hash", 100, Duration.ofDays(1), () -> {
                    computeCount.incrementAndGet();
                    return "computed once";
                });

        assertThat(result).isEqualTo("computed once");
        assertThat(computeCount).hasValue(1);
    }

    @Test
    void propagatesComputationFailureWithoutRetryingSupplier() {
        CompressionCacheRepository repository = Mockito.mock(CompressionCacheRepository.class);
        Mockito.when(repository.findByCacheKeyAndExpiresAtAfter(any(), any())).thenReturn(Optional.empty());
        CompressionCacheService service = new CompressionCacheService(repository);
        AtomicInteger computeCount = new AtomicInteger();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getOrCompute(
                        "cache-key", "v1", "generic", "default", "p1", "https://example.com", "hash",
                        100, Duration.ofDays(1), () -> {
                            computeCount.incrementAndGet();
                            throw new IllegalStateException("model failed");
                        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("model failed");
        assertThat(computeCount).hasValue(1);
    }
}
