package org.wrj.haifa.ai.deerflow.agent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.middleware.AgentMiddleware;
import org.wrj.haifa.ai.deerflow.middleware.AgentRuntimeContext;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewareChain;
import org.wrj.haifa.ai.deerflow.middleware.SummarizationMiddleware;
import org.wrj.haifa.ai.deerflow.middleware.ThreadMemoryMiddleware;
import org.wrj.haifa.ai.deerflow.middleware.ToolOutputBudgetMiddleware;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.EvidenceStore;
import org.wrj.haifa.ai.deerflow.research.ResearchEvidenceCompressor;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;
import org.wrj.haifa.ai.deerflow.research.ResearchSourceType;
import org.wrj.haifa.ai.deerflow.research.SourceRegistry;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchDimension;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchTaskStatus;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Phase7Tests {

    @Test
    void evidenceCompressorCompressesLongContentAndPreservesMetadata() {
        AgentModelClient modelClient = prompt -> Mono.just(new ModelResponse("Compressed content summary. Source ID: src-123."));
        ResearchEvidenceCompressor compressor = new ResearchEvidenceCompressor(modelClient);

        String result = compressor.compressFetchContent("Test Title", "http://example.com", "src-123", "Extra long page text content that goes on and on...");
        assertThat(result).contains("Compressed content summary")
                .contains("src-123");
    }

    @Test
    void toolOutputBudgetMiddlewareLimitsOutputAndCallsCompressor() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getToolOutputBudget().setEnabled(true);
        properties.getToolOutputBudget().setExternalizeEnabled(false); // disable externalization for test
        properties.getToolOutputBudget().setFallbackMaxChars(3000);
        properties.getToolOutputBudget().setFallbackHeadChars(1000);
        properties.getToolOutputBudget().setFallbackTailChars(1000);
        
        AgentModelClient modelClient = prompt -> Mono.just(new ModelResponse("Brief Summary"));
        ResearchEvidenceCompressor compressor = new ResearchEvidenceCompressor(modelClient);
        ToolOutputBudgetMiddleware middleware = new ToolOutputBudgetMiddleware(properties, compressor);

        // Short output should pass through unchanged
        String resultShort = middleware.processToolOutput("web_fetch", "hello", "t1", "r1", "http://url", "s1", "title");
        assertThat(resultShort).isEqualTo("hello");

        // Long output should be compressed by LLM first, then fallback if still too long
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4000; i++) {
            sb.append("a");
        }
        String resultLong = middleware.processToolOutput("web_fetch", sb.toString(), "t1", "r1", "http://url", "s1", "title");
        assertThat(resultLong).contains("Brief Summary").contains("Source ID: s1");
    }


    @Test
    void toolOutputBudgetMiddlewareUsesHeadTailFallbackWhenCompressionFails() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getToolOutputBudget().setEnabled(true);
        properties.getToolOutputBudget().setExternalizeEnabled(false);
        properties.getToolOutputBudget().setFallbackMaxChars(2000);
        properties.getToolOutputBudget().setFallbackHeadChars(700);
        properties.getToolOutputBudget().setFallbackTailChars(700);
        
        // Model client that throws exception to simulate LLM failure
        AgentModelClient failingClient = prompt -> Mono.error(new RuntimeException("Model timeout"));
        ResearchEvidenceCompressor compressor = new ResearchEvidenceCompressor(failingClient);
        ToolOutputBudgetMiddleware middleware = new ToolOutputBudgetMiddleware(properties, compressor);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append((i % 100 == 0) ? "\n" : "a");
        }
        String longContent = sb.toString();
        String result = middleware.processToolOutput("bash", longContent, "t1", "r1", null, null, null);
        
        // Should be head+tail truncated, not the full original
        assertThat(result.length()).isLessThan(longContent.length());
        assertThat(result).contains("chars omitted");
    }

    @Test
    void toolOutputBudgetMiddlewareSkipsAlreadyBudgetedEntries() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getToolOutputBudget().setEnabled(true);
        ToolOutputBudgetMiddleware middleware = new ToolOutputBudgetMiddleware(properties, null);

        assertThat(middleware.needsBudget("Tool result (bash): hello", "bash")).isTrue();
        assertThat(middleware.needsBudget("Tool result (bash): hello [Full bash output saved to /mnt/outputs/...", "bash")).isFalse();
        assertThat(middleware.needsBudget("Tool result (bash): hello [... 123 chars omitted from bash output...", "bash")).isFalse();
    }

    @Test
    void summarizationMiddlewareCompressesMessagesOnceThresholdExceeded() {
        MessageStore messageStore = mock(MessageStore.class);
        AgentModelClient modelClient = prompt -> Mono.just(new ModelResponse("Summary of history. source-1."));
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getGraph().setEnabled(false);
        properties.getSummarization().setEnabled(true);
        properties.getSummarization().setTriggerMessages(10);
        properties.getSummarization().setTriggerChars(8_000);
        properties.getSummarization().setKeepMessages(4);

        List<MessageRecord> messages = new ArrayList<>();
        // Add 12 messages to exceed threshold
        for (int i = 0; i < 12; i++) {
            messages.add(new MessageRecord("msg-" + i, "t1", "r1", MessageRole.USER, "Message " + i, Collections.emptyMap(), java.time.Instant.now()));
        }
        // Last message represents the current request
        messages.add(new MessageRecord("msg-last", "t1", "r1", MessageRole.USER, "Current request", Collections.emptyMap(), java.time.Instant.now()));

        when(messageStore.listByThread("t1")).thenReturn(messages);
        
        // Mock DB add for new summary message
        MessageRecord summaryRecord = new MessageRecord("msg-summary", "t1", "r1", MessageRole.SYSTEM, "Previous conversation summary (including source references):\nSummary of history. source-1.", Map.of("isSummary", true), java.time.Instant.now());

        when(messageStore.add(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyMap()
        )).thenReturn(summaryRecord);

        SummarizationMiddleware middleware = new SummarizationMiddleware(messageStore, modelClient, properties);

        AgentRunConfig config = new AgentRunConfig("t1", "r1", "model-1", false, false, 5, java.nio.file.Paths.get("."), RunMode.RESEARCH, null, java.util.Collections.emptyMap());
        AgentRequest request = new AgentRequest("t1", "Current request", "model-1");
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, Collections.emptyList(), new DeerFlowProperties());

        ModelPrompt initialPrompt = new ModelPrompt("System prompt", "Current request", "model-1");

        // Construct functional chain using anonymous AgentMiddleware
        MiddlewareChain chain = new MiddlewareChain(List.of(new AgentMiddleware() {
            @Override
            public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
                return Mono.just(initialPrompt);
            }
        }));

        ModelPrompt resultPrompt = middleware.apply(context, chain).block();

        assertThat(resultPrompt).isNotNull();
        assertThat(resultPrompt.userPrompt()).contains("Previous conversation summary")
                .contains("Message 11") // Last 4 messages are kept
                .contains("Current request");
    }

    @Test
    void summarizationMiddlewareInjectsThreadHistoryEvenWhenCompressionDisabled() {
        MessageStore messageStore = mock(MessageStore.class);
        AgentModelClient modelClient = prompt -> Mono.error(new AssertionError("summary model should not be called"));
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getGraph().setEnabled(false);
        properties.getSummarization().setEnabled(false);

        List<MessageRecord> messages = List.of(
                new MessageRecord("msg-1", "t1", "r-prev", MessageRole.USER,
                        "三家公司是阿里云、百度智能云、火山引擎。", Collections.emptyMap(), java.time.Instant.now()),
                new MessageRecord("msg-2", "t1", "r-prev", MessageRole.ASSISTANT,
                        "好的，我会按这三家公司比较主流模型版本。", Collections.emptyMap(), java.time.Instant.now()),
                new MessageRecord("msg-3", "t1", "r-current", MessageRole.USER,
                        "模型版本是当前这三家公司的几个主流模型版本，按token计费，中国内地部署，不需要附加需求。", Collections.emptyMap(), java.time.Instant.now())
        );
        when(messageStore.listByThread("t1")).thenReturn(messages);

        SummarizationMiddleware middleware = new SummarizationMiddleware(messageStore, modelClient, properties);
        AgentRunConfig config = new AgentRunConfig("t1", "r-current", "model-1", false, false, 5, java.nio.file.Paths.get("."), RunMode.RESEARCH, null, java.util.Collections.emptyMap());
        AgentRequest request = new AgentRequest("t1", "模型版本是当前这三家公司的几个主流模型版本，按token计费，中国内地部署，不需要附加需求。", "model-1");
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, Collections.emptyList(), properties);
        ModelPrompt initialPrompt = new ModelPrompt("System prompt", "User request:\n" + request.message(), "model-1");
        MiddlewareChain chain = new MiddlewareChain(List.of((ctx, next) -> Mono.just(initialPrompt)));

        ModelPrompt resultPrompt = middleware.apply(context, chain).block();

        assertThat(resultPrompt).isNotNull();
        assertThat(resultPrompt.userPrompt())
                .contains("<conversation_history>")
                .contains("三家公司是阿里云、百度智能云、火山引擎")
                .contains("好的，我会按这三家公司比较主流模型版本")
                .contains("User request:");
    }

    @Test
    void threadMemoryMiddlewareInjectsPreviousStateIntoSystemPrompt() {
        ResearchPlanStore planStore = mock(ResearchPlanStore.class);
        SourceRegistry sourceRegistry = mock(SourceRegistry.class);
        EvidenceStore evidenceStore = mock(EvidenceStore.class);

        ResearchDimension dim = new ResearchDimension("d1", "Dim 1", "Desc 1", ResearchTaskStatus.COMPLETED, Collections.emptyList(), 2, 2, 2, Collections.emptyList());
        ResearchPlan plan = new ResearchPlan("p1", "t1", "r1", "Test Topic", Collections.emptyList(), List.of(dim), Collections.emptyList(), "", "", "ACTIVE", null, null);
        when(planStore.findByThreadId("t1")).thenReturn(List.of(plan));

        ResearchSource src = new ResearchSource("s1", "t1", "r1", "Src 1", "http://url", "http://url", "url.com", null, Instant.now(), ResearchSourceType.OTHER, 0.9, "snippet", "hash");
        when(sourceRegistry.listByThread("t1")).thenReturn(List.of(src));

        EvidenceItem ev = new EvidenceItem("e1", "s1", "Src 1", "http://url", "quote", "claim", "Dim 1", 0.95f, null);
        when(evidenceStore.listByThread("t1")).thenReturn(List.of(ev));

        ThreadMemoryMiddleware middleware = new ThreadMemoryMiddleware(planStore, sourceRegistry, evidenceStore, null);

        AgentRunConfig config = new AgentRunConfig("t1", "r1", "model-1", false, false, 5, java.nio.file.Paths.get("."), RunMode.RESEARCH, null, java.util.Collections.emptyMap());
        AgentRequest request = new AgentRequest("t1", "User prompt", "model-1");
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, Collections.emptyList(), new DeerFlowProperties());

        ModelPrompt initialPrompt = new ModelPrompt("System prompt", "User prompt", "model-1");
        MiddlewareChain chain = new MiddlewareChain(List.of(new AgentMiddleware() {
            @Override
            public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
                return Mono.just(initialPrompt);
            }
        }));

        ModelPrompt resultPrompt = middleware.apply(context, chain).block();

        assertThat(resultPrompt).isNotNull();
        assertThat(resultPrompt.systemPrompt()).contains("Test Topic")
                .contains("Dim 1")
                .contains("Src 1")
                .contains("claim");
    }
}
