package org.wrj.haifa.ai.deerflow.graph;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.config.GraphRuntimeMode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wrj.haifa.ai.deerflow.agent.SimpleAgentRuntime.shouldUseActiveChatGraph;
import static org.wrj.haifa.ai.deerflow.agent.SimpleAgentRuntime.shouldUseActiveResearchGraph;

class GraphRuntimeModeRoutingTest {

    @Test
    void offModeDoesNotUseGraphRuntimes() {
        DeerFlowProperties properties = properties(GraphRuntimeMode.OFF);
        assertThat(shouldUseActiveChatGraph(properties, new GraphChatRuntime(), chatRequest())).isFalse();
        assertThat(shouldUseActiveResearchGraph(properties, new GraphResearchRuntime(), researchRequest())).isFalse();
    }

    @Test
    void activeChatModeRoutesToGraphChatRuntime() {
        DeerFlowProperties properties = properties(GraphRuntimeMode.ACTIVE_CHAT);
        assertThat(shouldUseActiveChatGraph(properties, new GraphChatRuntime(), chatRequest())).isTrue();
        assertThat(shouldUseActiveResearchGraph(properties, new GraphResearchRuntime(), chatRequest())).isFalse();
    }

    @Test
    void activeResearchModeRoutesToGraphResearchRuntime() {
        DeerFlowProperties properties = properties(GraphRuntimeMode.ACTIVE_RESEARCH);
        assertThat(shouldUseActiveResearchGraph(properties, new GraphResearchRuntime(), researchRequest())).isTrue();
        assertThat(shouldUseActiveChatGraph(properties, new GraphChatRuntime(), researchRequest())).isFalse();
    }

    @Test
    void shadowModeDoesNotUseActiveGraphs() {
        DeerFlowProperties properties = properties(GraphRuntimeMode.SHADOW);
        assertThat(shouldUseActiveChatGraph(properties, new GraphChatRuntime(), chatRequest())).isFalse();
        assertThat(shouldUseActiveResearchGraph(properties, new GraphResearchRuntime(), chatRequest())).isFalse();
    }

    @Test
    void chatRequestDoesNotRouteToResearchGraph() {
        DeerFlowProperties properties = properties(GraphRuntimeMode.ACTIVE_RESEARCH);
        assertThat(shouldUseActiveResearchGraph(properties, new GraphResearchRuntime(), chatRequest())).isFalse();
    }

    @Test
    void researchRequestDoesNotRouteToChatGraph() {
        DeerFlowProperties properties = properties(GraphRuntimeMode.ACTIVE_CHAT);
        assertThat(shouldUseActiveChatGraph(properties, new GraphChatRuntime(), researchRequest())).isFalse();
    }

    // Helper methods
    private static DeerFlowProperties properties(GraphRuntimeMode mode) {
        DeerFlowProperties p = new DeerFlowProperties();
        DeerFlowProperties.Graph gp = new DeerFlowProperties.Graph();
        gp.setEnabled(true);
        gp.setMode(mode);
        p.setGraph(gp);
        return p;
    }

    private static AgentRequest chatRequest() {
        return new AgentRequest("thread-1", "hello", "zhipu");
    }

    private static AgentRequest researchRequest() {
        return new AgentRequest("thread-1", "research topic", "zhipu", List.of(), RunMode.RESEARCH, ResearchOptions.defaults(), null, Map.of());
    }
}
