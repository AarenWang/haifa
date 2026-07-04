package org.wrj.haifa.ai.deerflow.graph.state;

public final class AgentGraphStateKeys {

    public static final String RUN_ID = "runId";
    public static final String THREAD_ID = "threadId";
    public static final String MODE = "mode";
    public static final String USER_ID = "userId";
    public static final String USER_MESSAGE = "userMessage";
    public static final String MODEL_NAME = "modelName";
    public static final String ACTIVE_SKILLS = "activeSkills";
    public static final String UPLOADED_FILE_IDS = "uploadedFileIds";
    public static final String REQUEST_METADATA = "requestMetadata";
    public static final String MESSAGE_WINDOW = "messageWindow";
    public static final String MODEL_PROMPT = "modelPrompt";
    public static final String MODEL_STEPS = "modelSteps";
    public static final String TOOL_CALLS = "toolCalls";
    public static final String TOOL_RESULTS = "toolResults";
    public static final String PENDING_TOOL_CALLS = "pendingToolCalls";
    public static final String TODOS = "todos";
    public static final String RESEARCH_PLAN_REF = "researchPlanRef";
    public static final String RESEARCH_OPTIONS = "researchOptions";
    public static final String RESEARCH_PHASE = "researchPhase";
    public static final String RESEARCH_SOURCE_COUNT = "sourceCount";
    public static final String RESEARCH_EVIDENCE_COUNT = "evidenceCount";
    public static final String RESEARCH_STEPS = "research_steps";
    public static final String QUALITY_GATE_PASSED = "quality_gate_passed";
    public static final String EMITTED_EVIDENCE_IDS = "emittedEvidenceIds";
    public static final String SUBAGENTS = "subagents";
    public static final String CLARIFICATION = "clarification";
    public static final String SANDBOX = "sandbox";
    public static final String ARTIFACTS = "artifacts";
    public static final String ERRORS = "errors";
    public static final String FINAL_ANSWER = "finalAnswer";
    public static final String USAGE = "usage";

    private AgentGraphStateKeys() {
    }
}
