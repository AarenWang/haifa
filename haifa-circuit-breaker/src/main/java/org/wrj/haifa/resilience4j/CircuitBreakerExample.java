package org.wrj.haifa.resilience4j;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;


public class CircuitBreakerExample {

    public static void main(String[] args) {

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .ringBufferSizeInHalfOpenState(2)
                .ringBufferSizeInClosedState(2)
                .recordExceptions(IOException.class, TimeoutException.class, ArithmeticException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();


        CircuitBreaker customCircuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);


       // 模拟失败调用，并链接降级函数
        CheckedFunction0<String> checkedSupplier = CircuitBreaker.decorateCheckedSupplier(customCircuitBreaker, () -> {
            throw new RuntimeException("BAM!");
        });
        Try<String> result = Try.of(checkedSupplier)
                .recover(throwable -> "Hello Recovery");

         // 降级函数被调用，最终调用结果为成功
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("Hello Recovery");


        checkedSupplier = CircuitBreaker.decorateCheckedSupplier(customCircuitBreaker, () -> {
            throw new IllegalArgumentException("BAM!");
        });
        result = Try.of(checkedSupplier)
                .recover(throwable -> "Hello IllegalArgumentException");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("Hello IllegalArgumentException");
    }
}
