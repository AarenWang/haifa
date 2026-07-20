package org.wrj.haifa.ai.deerflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "haifa.ai.deerflow.graph.executor")
public class GraphExecutorProperties {
    /**
     * Model/node pool settings kept under the original property names for backward compatibility.
     */
    private int corePoolSize = 8;
    private int maxPoolSize = 32;
    private int queueCapacity = 1000;
    private String threadNamePrefix = "graph-model-";

    private int coordinatorCorePoolSize = 4;
    private int coordinatorMaxPoolSize = 8;
    private int coordinatorQueueCapacity = 256;
    private String coordinatorThreadNamePrefix = "graph-coordinator-";

    private int toolCorePoolSize = 8;
    private int toolMaxPoolSize = 32;
    private int toolQueueCapacity = 512;
    private String toolThreadNamePrefix = "graph-tool-";

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    public int getCoordinatorCorePoolSize() {
        return coordinatorCorePoolSize;
    }

    public void setCoordinatorCorePoolSize(int coordinatorCorePoolSize) {
        this.coordinatorCorePoolSize = coordinatorCorePoolSize;
    }

    public int getCoordinatorMaxPoolSize() {
        return coordinatorMaxPoolSize;
    }

    public void setCoordinatorMaxPoolSize(int coordinatorMaxPoolSize) {
        this.coordinatorMaxPoolSize = coordinatorMaxPoolSize;
    }

    public int getCoordinatorQueueCapacity() {
        return coordinatorQueueCapacity;
    }

    public void setCoordinatorQueueCapacity(int coordinatorQueueCapacity) {
        this.coordinatorQueueCapacity = coordinatorQueueCapacity;
    }

    public String getCoordinatorThreadNamePrefix() {
        return coordinatorThreadNamePrefix;
    }

    public void setCoordinatorThreadNamePrefix(String coordinatorThreadNamePrefix) {
        this.coordinatorThreadNamePrefix = coordinatorThreadNamePrefix;
    }

    public int getToolCorePoolSize() {
        return toolCorePoolSize;
    }

    public void setToolCorePoolSize(int toolCorePoolSize) {
        this.toolCorePoolSize = toolCorePoolSize;
    }

    public int getToolMaxPoolSize() {
        return toolMaxPoolSize;
    }

    public void setToolMaxPoolSize(int toolMaxPoolSize) {
        this.toolMaxPoolSize = toolMaxPoolSize;
    }

    public int getToolQueueCapacity() {
        return toolQueueCapacity;
    }

    public void setToolQueueCapacity(int toolQueueCapacity) {
        this.toolQueueCapacity = toolQueueCapacity;
    }

    public String getToolThreadNamePrefix() {
        return toolThreadNamePrefix;
    }

    public void setToolThreadNamePrefix(String toolThreadNamePrefix) {
        this.toolThreadNamePrefix = toolThreadNamePrefix;
    }
}
