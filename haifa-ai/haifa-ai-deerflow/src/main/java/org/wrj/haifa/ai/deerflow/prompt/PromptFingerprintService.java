package org.wrj.haifa.ai.deerflow.prompt;

import java.util.List;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.model.ModelToolDefinition;
import org.wrj.haifa.ai.deerflow.model.cache.PromptBlock;
import org.wrj.haifa.ai.deerflow.model.cache.PromptBlockPlacement;
import org.wrj.haifa.ai.deerflow.model.cache.PromptBlockType;
import org.wrj.haifa.ai.deerflow.model.cache.PromptCacheability;
import org.wrj.haifa.ai.deerflow.model.cache.PromptFingerprint;

@Component
public class PromptFingerprintService {

    public PromptFingerprint computeFingerprint(
            List<PromptBlock> blocks,
            List<ModelToolDefinition> toolDefinitions,
            String conversationHistoryPrefix,
            String dynamicTail) {
        return computeFingerprint("v1", blocks, toolDefinitions, conversationHistoryPrefix, dynamicTail);
    }

    public PromptFingerprint computeFingerprint(
            String schemaVersion,
            List<PromptBlock> blocks,
            List<ModelToolDefinition> toolDefinitions,
            String conversationHistoryPrefix,
            String dynamicTail) {

        String staticSystemContent = extractBlockContent(blocks, PromptBlockType.STATIC_SYSTEM, PromptBlockType.SAFETY_POLICY, PromptBlockType.OUTPUT_POLICY, PromptBlockType.WORKSPACE_POLICY, PromptBlockType.TOOL_POLICY);
        String skillCatalogContent = extractBlockContent(blocks, PromptBlockType.SKILL_CATALOG);
        String activeSkillsContent = extractBlockContent(blocks, PromptBlockType.ACTIVE_SKILL);
        String sessionContextContent = extractBlockContent(blocks, PromptBlockType.THREAD_MEMORY, PromptBlockType.UPLOAD_MANIFEST);
        String canonicalToolsContent = PromptCanonicalizer.canonicalizeToolDefinitions(toolDefinitions);

        String staticSystemHash = PromptCanonicalizer.sha256Hex(staticSystemContent);
        String toolDefinitionsHash = PromptCanonicalizer.sha256Hex(canonicalToolsContent);
        String skillCatalogHash = PromptCanonicalizer.sha256Hex(skillCatalogContent);
        String activeSkillsHash = PromptCanonicalizer.sha256Hex(activeSkillsContent);
        String sessionContextHash = PromptCanonicalizer.sha256Hex(sessionContextContent);
        String conversationPrefixHash = PromptCanonicalizer.sha256Hex(PromptCanonicalizer.canonicalizeText(conversationHistoryPrefix));
        String dynamicTailHash = PromptCanonicalizer.sha256Hex(PromptCanonicalizer.canonicalizeText(dynamicTail));

        String cacheablePrefixConcat = PromptCanonicalizer.canonicalizeText(
                "staticSystem:" + staticSystemHash + "\n" +
                "tools:" + toolDefinitionsHash + "\n" +
                "skillCatalog:" + skillCatalogHash + "\n" +
                "activeSkills:" + activeSkillsHash + "\n" +
                "sessionContext:" + sessionContextHash
        );
        String cacheablePrefixHash = PromptCanonicalizer.sha256Hex(cacheablePrefixConcat);

        int cacheableChars = staticSystemContent.length() + canonicalToolsContent.length() + skillCatalogContent.length() + activeSkillsContent.length() + sessionContextContent.length();
        int totalChars = cacheableChars + (conversationHistoryPrefix == null ? 0 : conversationHistoryPrefix.length()) + (dynamicTail == null ? 0 : dynamicTail.length());

        int estimatedInputTokens = Math.max(1, totalChars / 4);
        int estimatedCacheablePrefixTokens = Math.max(0, cacheableChars / 4);

        return new PromptFingerprint(
                schemaVersion == null || schemaVersion.isBlank() ? "v1" : schemaVersion,
                staticSystemHash,
                toolDefinitionsHash,
                skillCatalogHash,
                activeSkillsHash,
                sessionContextHash,
                conversationPrefixHash,
                dynamicTailHash,
                cacheablePrefixHash,
                estimatedInputTokens,
                estimatedCacheablePrefixTokens
        );
    }

    private String extractBlockContent(List<PromptBlock> blocks, PromptBlockType... types) {
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (PromptBlockType type : types) {
            for (PromptBlock block : blocks) {
                if (block.type() == type && block.cacheability() == PromptCacheability.CACHEABLE) {
                    if (!sb.isEmpty()) {
                        sb.append("\n");
                    }
                    sb.append(PromptCanonicalizer.canonicalizeText(block.content()));
                }
            }
        }
        return sb.toString();
    }
}
