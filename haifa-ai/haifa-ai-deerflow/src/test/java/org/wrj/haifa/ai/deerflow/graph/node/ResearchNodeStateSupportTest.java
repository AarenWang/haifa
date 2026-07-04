package org.wrj.haifa.ai.deerflow.graph.node;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.ResearchDepth;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.ResearchOutputFormat;
import org.wrj.haifa.ai.deerflow.agent.ResearchTimeWindow;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchNodeStateSupportTest {

    @Test
    void parsesCheckpointFriendlyResearchOptions() {
        ResearchOptions options = ResearchNodeStateSupport.researchOptions(Map.of(
                "depth", "deep",
                "timeWindow", "last-year",
                "maxSources", "12",
                "requireCitations", "false",
                "outputFormat", "report"
        ));

        assertThat(options.depth()).isEqualTo(ResearchDepth.DEEP);
        assertThat(options.timeWindow()).isEqualTo(ResearchTimeWindow.LAST_YEAR);
        assertThat(options.maxSources()).isEqualTo(12);
        assertThat(options.requireCitations()).isFalse();
        assertThat(options.outputFormat()).isEqualTo(ResearchOutputFormat.REPORT);
    }

    @Test
    void fallsBackToResearchOptionDefaultsForInvalidValues() {
        ResearchOptions options = ResearchNodeStateSupport.researchOptions(Map.of(
                "depth", "unknown",
                "timeWindow", "all-time",
                "requireCitations", "not-a-boolean"
        ));

        assertThat(options.depth()).isEqualTo(ResearchDepth.STANDARD);
        assertThat(options.timeWindow()).isEqualTo(ResearchTimeWindow.ALL_TIME);
        assertThat(options.maxSources()).isEqualTo(10);
        assertThat(options.requireCitations()).isTrue();
        assertThat(options.outputFormat()).isEqualTo(ResearchOutputFormat.ANSWER);
    }
}
