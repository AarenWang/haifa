package org.wrj.haifa.ai.deerflow.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelToolDefinition;
import org.wrj.haifa.ai.deerflow.model.cache.PromptCacheEligibility;

class PromptCachePlannerTest {

    @Test
    void keepsCacheablePrefixStableWhenOnlyDynamicTailChanges() {
        DeerFlowProperties properties = new DeerFlowProperties();
        String longStableSystem = "stable policy ".repeat(500);
        List<ModelToolDefinition> tools = List.of(
                new ModelToolDefinition("search", "Search", "{\"type\":\"object\"}"));

        ModelPrompt first = PromptCachePlanner.enrich(new ModelPrompt(
                longStableSystem, "", "gpt-4.1",
                List.of(new ModelMessage(ModelMessage.Role.USER, "first turn")), tools), properties);
        ModelPrompt second = PromptCachePlanner.enrich(new ModelPrompt(
                longStableSystem, "", "gpt-4.1",
                List.of(new ModelMessage(ModelMessage.Role.USER, "second turn")), tools), properties);

        assertThat(first.cacheContext().enabled()).isTrue();
        assertThat(first.cacheContext().eligibility()).isEqualTo(PromptCacheEligibility.ELIGIBLE);
        assertThat(first.cacheContext().routingKey()).isEqualTo(second.cacheContext().routingKey());
        assertThat(first.cacheContext().fingerprint().cacheablePrefixHash())
                .isEqualTo(second.cacheContext().fingerprint().cacheablePrefixHash());
        assertThat(first.cacheContext().fingerprint().dynamicTailHash())
                .isNotEqualTo(second.cacheContext().fingerprint().dynamicTailHash());
    }

    @Test
    void toolOrderingDoesNotChangeFingerprint() {
        DeerFlowProperties properties = new DeerFlowProperties();
        ModelToolDefinition a = new ModelToolDefinition("a", "A", "{\"b\":2,\"a\":1}");
        ModelToolDefinition b = new ModelToolDefinition("b", "B", "{\"type\":\"object\"}");

        ModelPrompt first = PromptCachePlanner.enrich(
                new ModelPrompt("system", "user", "gpt-4.1", List.of(), List.of(a, b)), properties);
        ModelPrompt second = PromptCachePlanner.enrich(
                new ModelPrompt("system", "user", "gpt-4.1", List.of(), List.of(b, a)), properties);

        assertThat(first.cacheContext().fingerprint().toolDefinitionsHash())
                .isEqualTo(second.cacheContext().fingerprint().toolDefinitionsHash());
        assertThat(first.toolDefinitions()).extracting(ModelToolDefinition::name).containsExactly("a", "b");
        assertThat(second.toolDefinitions()).extracting(ModelToolDefinition::name).containsExactly("a", "b");
    }

    @Test
    void globalDisablePreservesFingerprintButDisablesRouting() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getPromptCache().setEnabled(false);

        ModelPrompt enriched = PromptCachePlanner.enrich(
                new ModelPrompt("system", "user", "gpt-4.1"), properties);

        assertThat(enriched.cacheContext().enabled()).isFalse();
        assertThat(enriched.cacheContext().eligibility()).isEqualTo(PromptCacheEligibility.DISABLED);
        assertThat(enriched.cacheContext().routingKey()).isEmpty();
        assertThat(enriched.cacheContext().fingerprint().cacheablePrefixHash()).isNotBlank();
    }
}
