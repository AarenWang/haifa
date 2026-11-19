package org.wrj.haifa.ai.deerflow.mcp;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Run-isolated selections returned by tool_search for large MCP catalogs. */
@Component
public class McpToolSelectionStore {

    private static final int MAX_RUNS = 10_000;
    private static final int MAX_SELECTED_PER_RUN = 32;
    private final ConcurrentHashMap<String, Selection> selections = new ConcurrentHashMap<>();

    public void select(String runId, Collection<String> exposedNames) {
        if (!StringUtils.hasText(runId) || exposedNames == null) return;
        LinkedHashSet<String> safe = new LinkedHashSet<>();
        exposedNames.stream().filter(name -> name != null && name.startsWith("mcp__"))
                .sorted().limit(MAX_SELECTED_PER_RUN).forEach(safe::add);
        selections.put(runId, new Selection(Set.copyOf(safe), Instant.now()));
        if (selections.size() > MAX_RUNS) {
            Instant cutoff = Instant.now().minusSeconds(3600);
            selections.entrySet().removeIf(entry -> entry.getValue().selectedAt().isBefore(cutoff));
        }
    }

    public Set<String> selected(String runId) {
        Selection selection = selections.get(runId);
        return selection == null ? Set.of() : selection.names();
    }

    public void clear(String runId) {
        if (runId != null) selections.remove(runId);
    }

    private record Selection(Set<String> names, Instant selectedAt) { }
}
