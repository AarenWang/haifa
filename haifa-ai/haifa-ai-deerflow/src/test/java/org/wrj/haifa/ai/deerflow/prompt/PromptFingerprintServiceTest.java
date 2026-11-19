package org.wrj.haifa.ai.deerflow.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.model.ModelToolDefinition;
import org.wrj.haifa.ai.deerflow.model.cache.PromptBlock;
import org.wrj.haifa.ai.deerflow.model.cache.PromptBlockPlacement;
import org.wrj.haifa.ai.deerflow.model.cache.PromptBlockType;
import org.wrj.haifa.ai.deerflow.model.cache.PromptCacheability;
import org.wrj.haifa.ai.deerflow.model.cache.PromptFingerprint;
import org.wrj.haifa.ai.deerflow.model.cache.PromptStability;

class PromptFingerprintServiceTest {

    private final PromptFingerprintService service = new PromptFingerprintService();

    @Test
    void cacheablePrefixHashIsStableAcrossDynamicTailChanges() {
        PromptBlock staticSystem = new PromptBlock(
                PromptBlockType.STATIC_SYSTEM,
                PromptBlockPlacement.SYSTEM_PREFIX,
                PromptStability.GLOBAL,
                PromptCacheability.CACHEABLE,
                "v1",
                "You are DeerFlow lead agent."
        );

        List<ModelToolDefinition> tools = List.of(
                new ModelToolDefinition("web_search", "Search web", "{}")
        );

        PromptFingerprint fp1 = service.computeFingerprint(List.of(staticSystem), tools, "History turn 1", "User Query 1, Date: 2026-07-19T01:00:00Z");
        PromptFingerprint fp2 = service.computeFingerprint(List.of(staticSystem), tools, "History turn 1", "User Query 2, Date: 2026-07-19T02:30:00Z");

        assertThat(fp1.cacheablePrefixHash()).isEqualTo(fp2.cacheablePrefixHash());
        assertThat(fp1.staticSystemHash()).isEqualTo(fp2.staticSystemHash());
        assertThat(fp1.toolDefinitionsHash()).isEqualTo(fp2.toolDefinitionsHash());
        assertThat(fp1.dynamicTailHash()).isNotEqualTo(fp2.dynamicTailHash());
    }

    @Test
    void toolDefinitionOrderChangeDoesNotChangeToolDefinitionsHash() {
        PromptBlock staticSystem = new PromptBlock(
                PromptBlockType.STATIC_SYSTEM,
                PromptBlockPlacement.SYSTEM_PREFIX,
                PromptStability.GLOBAL,
                PromptCacheability.CACHEABLE,
                "v1",
                "System prompt"
        );

        ModelToolDefinition tool1 = new ModelToolDefinition("alpha", "Alpha tool", "{}");
        ModelToolDefinition tool2 = new ModelToolDefinition("beta", "Beta tool", "{}");

        PromptFingerprint fp1 = service.computeFingerprint(List.of(staticSystem), List.of(tool1, tool2), "", "Tail");
        PromptFingerprint fp2 = service.computeFingerprint(List.of(staticSystem), List.of(tool2, tool1), "", "Tail");

        assertThat(fp1.toolDefinitionsHash()).isEqualTo(fp2.toolDefinitionsHash());
        assertThat(fp1.cacheablePrefixHash()).isEqualTo(fp2.cacheablePrefixHash());
    }
}
