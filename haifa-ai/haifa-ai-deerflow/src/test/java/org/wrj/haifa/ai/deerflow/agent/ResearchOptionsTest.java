package org.wrj.haifa.ai.deerflow.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchOptionsTest {

    @Test
    void defaultsAreApplied() {
        ResearchOptions opts = ResearchOptions.defaults();
        assertThat(opts.depth()).isEqualTo(ResearchDepth.STANDARD);
        assertThat(opts.timeWindow()).isEqualTo(ResearchTimeWindow.LATEST);
        assertThat(opts.maxSources()).isEqualTo(10);
        assertThat(opts.requireCitations()).isTrue();
        assertThat(opts.outputFormat()).isEqualTo(ResearchOutputFormat.ANSWER);
    }

    @Test
    void quickPreset() {
        ResearchOptions opts = ResearchOptions.quick();
        assertThat(opts.depth()).isEqualTo(ResearchDepth.QUICK);
        assertThat(opts.maxSources()).isEqualTo(5);
        assertThat(opts.requireCitations()).isFalse();
        assertThat(opts.outputFormat()).isEqualTo(ResearchOutputFormat.ANSWER);
    }

    @Test
    void deepPreset() {
        ResearchOptions opts = ResearchOptions.deep();
        assertThat(opts.depth()).isEqualTo(ResearchDepth.DEEP);
        assertThat(opts.maxSources()).isEqualTo(20);
        assertThat(opts.requireCitations()).isTrue();
        assertThat(opts.outputFormat()).isEqualTo(ResearchOutputFormat.REPORT);
    }

    @Test
    void nullFieldsGetDefaults() {
        ResearchOptions opts = new ResearchOptions(null, null, null, null, null);
        assertThat(opts.depth()).isEqualTo(ResearchDepth.STANDARD);
        assertThat(opts.timeWindow()).isEqualTo(ResearchTimeWindow.LATEST);
        assertThat(opts.maxSources()).isEqualTo(10);
        assertThat(opts.requireCitations()).isTrue();
        assertThat(opts.outputFormat()).isEqualTo(ResearchOutputFormat.ANSWER);
    }

    @Test
    void explicitValuesArePreserved() {
        ResearchOptions opts = new ResearchOptions(
                ResearchDepth.DEEP,
                ResearchTimeWindow.ALL_TIME,
                25,
                Boolean.FALSE,
                ResearchOutputFormat.REPORT
        );
        assertThat(opts.depth()).isEqualTo(ResearchDepth.DEEP);
        assertThat(opts.timeWindow()).isEqualTo(ResearchTimeWindow.ALL_TIME);
        assertThat(opts.maxSources()).isEqualTo(25);
        assertThat(opts.requireCitations()).isFalse();
        assertThat(opts.outputFormat()).isEqualTo(ResearchOutputFormat.REPORT);
    }
}
