package org.wrj.haifa.ai.deerflow.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.GraphExecutorProperties;

import java.util.concurrent.Executor;

@Component
public class GraphExecutionManager implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(GraphExecutionManager.class);

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private final GraphExecutorProperties properties;
    private ThreadPoolTaskExecutor executor;

    @Autowired
    public GraphExecutionManager(GraphExecutorProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Initializing GraphExecutionManager thread pool. coreSize={}, maxSize={}, queueCapacity={}",
                properties.getCorePoolSize(), properties.getMaxPoolSize(), properties.getQueueCapacity());
        this.executor = new ThreadPoolTaskExecutor();
        this.executor.setCorePoolSize(properties.getCorePoolSize());
        this.executor.setMaxPoolSize(properties.getMaxPoolSize());
        this.executor.setQueueCapacity(properties.getQueueCapacity());
        this.executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        this.executor.setWaitForTasksToCompleteOnShutdown(true);
        this.executor.setAwaitTerminationSeconds(30);
        this.executor.initialize();
    }

    public Executor getExecutor() {
        return this.executor == null ? DIRECT_EXECUTOR : this.executor;
    }

    public static Executor fallbackExecutor() {
        return DIRECT_EXECUTOR;
    }

    @Override
    public void destroy() {
        if (this.executor != null) {
            log.info("Shutting down GraphExecutionManager executor...");
            this.executor.shutdown();
        }
    }
}
