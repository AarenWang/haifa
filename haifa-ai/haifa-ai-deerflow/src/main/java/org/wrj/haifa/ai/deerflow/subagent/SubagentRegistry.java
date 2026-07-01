package org.wrj.haifa.ai.deerflow.subagent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

/**
 * Registry of available subagent configurations.
 *
 * <p>Built-in subagents (general-purpose, bash) are always registered.
 * Custom subagents can be configured via application properties.
 */
@Component
public class SubagentRegistry {

    private final Map<String, SubagentConfig> builtinSubagents;
    private final Map<String, SubagentConfig> customSubagents;
    private final boolean bashEnabled;

    public SubagentRegistry(DeerFlowProperties properties) {
        this.bashEnabled = properties.isBashEnabled();
        Map<String, SubagentConfig> builtins = new ConcurrentHashMap<>();
        builtins.put("general-purpose", SubagentConfig.generalPurpose());
        if (bashEnabled) {
            builtins.put("bash", SubagentConfig.bash());
        }
        this.builtinSubagents = Collections.unmodifiableMap(builtins);
        this.customSubagents = new ConcurrentHashMap<>();
        // Future: load custom subagents from properties
    }

    public SubagentConfig getConfig(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        SubagentConfig config = builtinSubagents.get(name);
        if (config != null) {
            return config;
        }
        return customSubagents.get(name);
    }

    public List<String> getAvailableNames() {
        List<String> names = new ArrayList<>(builtinSubagents.keySet());
        names.addAll(customSubagents.keySet());
        return Collections.unmodifiableList(names);
    }

    public List<String> getBuiltinNames() {
        return List.copyOf(builtinSubagents.keySet());
    }

    public boolean isAvailable(String name) {
        return getConfig(name) != null;
    }

    public void registerCustom(SubagentConfig config) {
        if (config != null && config.name() != null && !config.name().isBlank()) {
            customSubagents.put(config.name(), config);
        }
    }

    public boolean isBashEnabled() {
        return bashEnabled;
    }
}
