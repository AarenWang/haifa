package org.wrj.haifa.ai.deerflow.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class GraphCoreSmokeTest {

    @Test
    void compilesInvokesStreamsAndCheckpointsMinimalGraph() throws Exception {
        MemorySaver saver = new MemorySaver();
        CompiledGraph graph = minimalGraph().compile(CompileConfig.builder()
                .recursionLimit(10)
                .saverConfig(SaverConfig.builder().register(saver).build())
                .build());

        RunnableConfig invokeConfig = RunnableConfig.builder()
                .threadId("phase17a-invoke-thread")
                .build();
        Map<String, Object> input = Map.of("messages", List.of("input"));

        OverAllState finalState = graph.invoke(input, invokeConfig).orElseThrow();

        assertThat(finalState.<List<String>>value("messages").orElseThrow())
                .containsExactly("input", "first", "second");
        assertThat(finalState.<String>value("summary").orElseThrow())
                .isEqualTo("input,first");
        assertThat(saver.list(invokeConfig)).isNotEmpty();
        assertThat(graph.getStateHistory(invokeConfig)).isNotEmpty();

        RunnableConfig streamConfig = RunnableConfig.builder()
                .threadId("phase17a-stream-thread")
                .build();
        List<String> streamedNodes = graph.stream(input, streamConfig)
                .map(NodeOutput::node)
                .collectList()
                .block(Duration.ofSeconds(5));

        assertThat(streamedNodes).contains("first", "second");
    }

    private StateGraph minimalGraph() throws Exception {
        StateGraph graph = new StateGraph("phase17a-smoke", KeyStrategy.builder()
                .defaultStrategy(KeyStrategy.REPLACE)
                .addStrategy("messages", KeyStrategy.APPEND)
                .addStrategy("summary", KeyStrategy.REPLACE)
                .build());

        graph.addNode("first", state -> CompletableFuture.completedFuture(Map.of(
                "messages", List.of("first")
        )));
        graph.addNode("second", state -> {
            List<String> messages = state.<List<String>>value("messages").orElse(List.of());
            return CompletableFuture.completedFuture(Map.of(
                    "messages", List.of("second"),
                    "summary", String.join(",", messages)
            ));
        });
        graph.addEdge(StateGraph.START, "first")
                .addEdge("first", "second")
                .addEdge("second", StateGraph.END);
        return graph;
    }
}
