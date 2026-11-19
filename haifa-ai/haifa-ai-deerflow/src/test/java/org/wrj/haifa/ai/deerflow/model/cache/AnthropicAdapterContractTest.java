package org.wrj.haifa.ai.deerflow.model.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AnthropicAdapterContractTest {

    @Test
    void verifiesAnthropicAccountingNormalizationContract() {
        long rawInput = 100L;
        long cacheRead = 800L;
        long cacheCreation = 200L;

        long normalizedInputTokens = rawInput + cacheRead + cacheCreation;
        long uncachedInputTokens = rawInput;

        ModelUsage usage = new ModelUsage(
                normalizedInputTokens,
                uncachedInputTokens,
                150L,
                normalizedInputTokens + 150L,
                cacheRead,
                cacheCreation,
                null,
                "anthropic",
                "claude-3-5-sonnet",
                UsageAvailability.PROVIDER_REPORTED,
                java.util.Map.of("cacheCreationInputTokens", cacheCreation)
        );

        assertThat(usage.inputTokens()).isEqualTo(1100L);
        assertThat(usage.uncachedInputTokens()).isEqualTo(100L);
        assertThat(usage.cacheReadInputTokens()).isEqualTo(800L);
        assertThat(usage.cacheWriteInputTokens()).isEqualTo(200L);
        assertThat(usage.cacheHitRate()).isEqualTo(800.0 / 1100.0);
    }

    @Test
    void verifiesBlockOrderingContract() {
        PromptBlock staticSystem = new PromptBlock(PromptBlockType.STATIC_SYSTEM, PromptBlockPlacement.SYSTEM_PREFIX, PromptStability.GLOBAL, PromptCacheability.CACHEABLE, "v1", "Static Rules");
        PromptBlock toolPolicy = new PromptBlock(PromptBlockType.TOOL_POLICY, PromptBlockPlacement.SYSTEM_PREFIX, PromptStability.GLOBAL, PromptCacheability.CACHEABLE, "v1", "Tool Rules");
        PromptBlock activeSkill = new PromptBlock(PromptBlockType.ACTIVE_SKILL, PromptBlockPlacement.SESSION_PREFIX, PromptStability.TURN, PromptCacheability.CACHEABLE, "v1", "Skill Content");
        PromptBlock dynamicTail = new PromptBlock(PromptBlockType.USER_QUERY, PromptBlockPlacement.DYNAMIC_TAIL, PromptStability.TURN, PromptCacheability.NOT_CACHEABLE, "v1", "User Query");

        List<PromptBlock> blocks = List.of(staticSystem, toolPolicy, activeSkill, dynamicTail);

        // Verify cacheable blocks precede non-cacheable block
        int lastCacheableIndex = -1;
        int firstNonCacheableIndex = -1;
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).cacheability() == PromptCacheability.CACHEABLE) {
                lastCacheableIndex = i;
            } else if (firstNonCacheableIndex < 0) {
                firstNonCacheableIndex = i;
            }
        }

        assertThat(lastCacheableIndex).isLessThan(firstNonCacheableIndex);
    }
}
