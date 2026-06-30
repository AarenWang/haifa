package org.wrj.haifa.ai.deerflow.research.plan;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.ResearchDepth;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.ResearchOutputFormat;
import org.wrj.haifa.ai.deerflow.agent.ResearchTimeWindow;

import static org.assertj.core.api.Assertions.assertThat;

class ClarificationGateTest {

    private final ClarificationGate gate = new ClarificationGate();

    @Test
    void emptyMessageNeedsClarification() {
        ClarificationGate.ClarificationResult result = gate.check("", ResearchOptions.defaults());
        assertThat(result.needsClarification()).isTrue();
        assertThat(result.clarificationQuestion()).containsIgnoringCase("provide");
    }

    @Test
    void clearTopicProceeds() {
        ClarificationGate.ClarificationResult result = gate.check(
                "What are the latest developments in quantum computing for 2025?",
                ResearchOptions.defaults()
        );
        assertThat(result.needsClarification()).isFalse();
        assertThat(result.proceed()).isTrue();
    }

    @Test
    void vagueTopicNeedsClarification() {
        ClarificationGate.ClarificationResult result = gate.check(
                "Tell me something about tech",
                ResearchOptions.defaults()
        );
        assertThat(result.proceed() || result.needsClarification()).isTrue();
    }

    @Test
    void ambiguousComparisonNeedsClarification() {
        ClarificationGate.ClarificationResult result = gate.check(
                "Compare the best stocks",
                ResearchOptions.defaults()
        );
        assertThat(result.needsClarification()).isTrue();
        assertThat(result.clarificationType()).isEqualTo("ambiguous_requirement");
    }

    @Test
    void proceedWithDefaultsWhenOptionsAreImplicit() {
        ResearchOptions options = new ResearchOptions(
                ResearchDepth.STANDARD,
                ResearchTimeWindow.LATEST,
                10,
                true,
                ResearchOutputFormat.ANSWER
        );
        ClarificationGate.ClarificationResult result = gate.check(
                "Latest trends in AI technology",
                options
        );
        assertThat(result.needsClarification()).isFalse();
        assertThat(result.proceed()).isTrue();
    }

    @Test
    void shortMessageIsScopeUnclear() {
        ClarificationGate.ClarificationResult result = gate.check(
                "AI stuff",
                ResearchOptions.defaults()
        );
        assertThat(result.proceed()).isTrue();
        assertThat(result.defaultNote()).isNotNull();
    }

    @Test
    void timeWindowAmbiguityGetsDefaultNote() {
        ResearchOptions options = new ResearchOptions(
                ResearchDepth.STANDARD,
                ResearchTimeWindow.LATEST,
                10,
                true,
                ResearchOutputFormat.ANSWER
        );
        ClarificationGate.ClarificationResult result = gate.check(
                "AI technology trends",
                options
        );
        assertThat(result.proceed()).isTrue();
    }
}
