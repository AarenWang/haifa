package org.wrj.haifa.ai.deerflow.tool.execution;

import java.util.List;

public record ToolBatchResult<T>(List<ToolBatchItemResult<T>> items, boolean cancelled) {
    public ToolBatchResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
