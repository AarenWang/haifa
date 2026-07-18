package org.wrj.haifa.ai.deerflow.model.cache;

public enum PromptBlockType {
    STATIC_SYSTEM,
    SAFETY_POLICY,
    OUTPUT_POLICY,
    WORKSPACE_POLICY,
    TOOL_POLICY,
    SKILL_CATALOG,
    ACTIVE_SKILL,
    THREAD_MEMORY,
    UPLOAD_MANIFEST,
    DYNAMIC_CONTEXT,
    USER_QUERY,
    RETRY_INSTRUCTION,
    APPROVAL_RESULT
}
