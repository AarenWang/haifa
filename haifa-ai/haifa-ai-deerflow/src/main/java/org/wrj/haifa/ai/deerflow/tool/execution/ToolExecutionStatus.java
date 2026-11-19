package org.wrj.haifa.ai.deerflow.tool.execution;

/** Durable state machine for a side-effecting tool action. */
public enum ToolExecutionStatus {
    RESERVED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    UNKNOWN_OUTCOME
}
