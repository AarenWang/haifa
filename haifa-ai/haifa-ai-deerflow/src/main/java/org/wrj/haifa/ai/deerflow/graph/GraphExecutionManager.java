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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class GraphExecutionManager implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(GraphExecutionManager.class);

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private final GraphExecutorProperties properties;
    private ThreadPoolTaskExecutor coordinatorExecutor;
    private ThreadPoolTaskExecutor modelExecutor;
    private ThreadPoolTaskExecutor toolExecutor;
    private final AtomicLong coordinatorRejected = new AtomicLong();
    private final AtomicLong modelRejected = new AtomicLong();
    private final AtomicLong toolRejected = new AtomicLong();

    @Autowired
    public GraphExecutionManager(GraphExecutorProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        validate("coordinator", properties.getCoordinatorCorePoolSize(), properties.getCoordinatorMaxPoolSize(),
                properties.getCoordinatorQueueCapacity(), properties.getCoordinatorThreadNamePrefix());
        validate("model", properties.getCorePoolSize(), properties.getMaxPoolSize(),
                properties.getQueueCapacity(), properties.getThreadNamePrefix());
        validate("tool", properties.getToolCorePoolSize(), properties.getToolMaxPoolSize(),
                properties.getToolQueueCapacity(), properties.getToolThreadNamePrefix());
        this.coordinatorExecutor = create("coordinator", properties.getCoordinatorCorePoolSize(),
                properties.getCoordinatorMaxPoolSize(), properties.getCoordinatorQueueCapacity(),
                properties.getCoordinatorThreadNamePrefix());
        this.modelExecutor = create("model", properties.getCorePoolSize(), properties.getMaxPoolSize(),
                properties.getQueueCapacity(), properties.getThreadNamePrefix());
        this.toolExecutor = create("tool", properties.getToolCorePoolSize(), properties.getToolMaxPoolSize(),
                properties.getToolQueueCapacity(), properties.getToolThreadNamePrefix());
        log.info("Initialized isolated Graph executors. coordinator={}, model={}, tool={}",
                status("coordinator", coordinatorExecutor, coordinatorRejected),
                status("model", modelExecutor, modelRejected), status("tool", toolExecutor, toolRejected));
    }

    /** Compatibility accessor for existing non-tool nodes. */
    public Executor getExecutor() {
        return getModelExecutor();
    }

    public Executor getCoordinatorExecutor() {
        return contextualExecutor("", "coordinator", coordinatorExecutor, coordinatorRejected);
    }

    public Executor getCoordinatorExecutor(String runId) {
        return contextualExecutor(runId, "coordinator", coordinatorExecutor, coordinatorRejected);
    }

    public Executor getModelExecutor() {
        return contextualExecutor("", "model", modelExecutor, modelRejected);
    }

    public Executor getModelExecutor(String runId) {
        return contextualExecutor(runId, "model", modelExecutor, modelRejected);
    }

    public Executor getToolExecutor() {
        return contextualExecutor("", "tool", toolExecutor, toolRejected);
    }

    public Executor getToolExecutor(String runId) {
        return contextualExecutor(runId, "tool", toolExecutor, toolRejected);
    }

    public ExecutorStatus coordinatorStatus() {
        return status("coordinator", coordinatorExecutor, coordinatorRejected);
    }

    public ExecutorStatus modelStatus() {
        return status("model", modelExecutor, modelRejected);
    }

    public ExecutorStatus toolStatus() {
        return status("tool", toolExecutor, toolRejected);
    }

    public static Executor fallbackExecutor() {
        return DIRECT_EXECUTOR;
    }

    @Override
    public void destroy() {
        shutdown("coordinator", coordinatorExecutor);
        shutdown("model", modelExecutor);
        shutdown("tool", toolExecutor);
    }

    private ThreadPoolTaskExecutor create(String name, int core, int max, int queue, String prefix) {
        ThreadPoolTaskExecutor result = new ThreadPoolTaskExecutor();
        result.setCorePoolSize(core);
        result.setMaxPoolSize(max);
        result.setQueueCapacity(queue);
        result.setThreadNamePrefix(prefix);
        result.setWaitForTasksToCompleteOnShutdown(true);
        result.setAwaitTerminationSeconds(30);
        result.initialize();
        log.info("Initialized {} executor. coreSize={}, maxSize={}, queueCapacity={}", name, core, max, queue);
        return result;
    }

    private Executor contextualExecutor(String runId, String name, ThreadPoolTaskExecutor delegate,
            AtomicLong rejected) {
        if (delegate == null) {
            return DIRECT_EXECUTOR;
        }
        return task -> {
            try {
                delegate.execute(task);
            }
            catch (RuntimeException ex) {
                rejected.incrementAndGet();
                ExecutorStatus current = status(name, delegate, rejected);
                log.error("Graph executor rejected task. runId={}, executor={}, active={}, queue={}, rejected={}",
                        runId, name, current.activeCount(), current.queueSize(), current.rejectedCount(), ex);
                throw new GraphExecutorRejectedException(runId, current, ex);
            }
        };
    }

    private static ExecutorStatus status(String name, ThreadPoolTaskExecutor executor, AtomicLong rejected) {
        if (executor == null || executor.getThreadPoolExecutor() == null) {
            return new ExecutorStatus(name, 0, 0, 0, rejected.get(), true);
        }
        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
        return new ExecutorStatus(name, pool.getActiveCount(), pool.getQueue().size(), pool.getPoolSize(),
                rejected.get(), pool.isShutdown());
    }

    private static void validate(String name, int core, int max, int queue, String prefix) {
        if (core <= 0 || max <= 0 || queue <= 0 || core > max || prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("Invalid " + name + " executor configuration: core=" + core
                    + ", max=" + max + ", queue=" + queue + ", prefix=" + prefix);
        }
    }

    private static void shutdown(String name, ThreadPoolTaskExecutor executor) {
        if (executor != null) {
            log.info("Shutting down {} executor. status={}", name,
                    status(name, executor, new AtomicLong()));
            executor.shutdown();
        }
    }

    public record ExecutorStatus(String name, int activeCount, int queueSize, int poolSize,
                                 long rejectedCount, boolean shutdown) {
    }
}
