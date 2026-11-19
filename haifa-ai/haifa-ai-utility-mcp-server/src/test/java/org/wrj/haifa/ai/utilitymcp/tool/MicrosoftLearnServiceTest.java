package org.wrj.haifa.ai.utilitymcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityErrorCode;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityResult;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

class MicrosoftLearnServiceTest {

    @Test
    void mapsOnlyTheThreeStableContractsToTheOfficialToolArguments() {
        Map<String, Object> captured = new LinkedHashMap<>();
        MicrosoftLearnService service = new MicrosoftLearnService((toolName, arguments) -> {
            captured.put("tool", toolName);
            captured.put("arguments", arguments);
            return UtilityResult.local(Map.of("content", "fixture"));
        });

        service.searchDocs(Map.of("query", "Azure Functions timeout"));
        assertThat(captured).containsEntry("tool", "microsoft_docs_search")
                .containsEntry("arguments", Map.of("query", "Azure Functions timeout"));

        service.fetchDocs(Map.of("url", "https://learn.microsoft.com/en-us/azure/azure-functions/functions-scale"));
        assertThat(captured).containsEntry("tool", "microsoft_docs_fetch")
                .containsEntry("arguments", Map.of("url", "https://learn.microsoft.com/en-us/azure/azure-functions/functions-scale"));

        service.searchCodeSamples(Map.of("query", "Azure Storage SDK", "language", "C#"));
        assertThat(captured).containsEntry("tool", "microsoft_code_sample_search")
                .containsEntry("arguments", Map.of("query", "Azure Storage SDK", "language", "C#"));
    }

    @Test
    void rejectsNonMicrosoftLearnFetchUrlsBeforeCallingTheRemoteMcp() {
        MicrosoftLearnService service = new MicrosoftLearnService((toolName, arguments) -> {
            throw new AssertionError("remote MCP must not be called");
        });

        assertThatThrownBy(() -> service.fetchDocs(Map.of("url", "https://example.com/not-allowed")))
                .isInstanceOfSatisfying(UtilityToolException.class,
                        ex -> assertThat(ex.code()).isEqualTo(UtilityErrorCode.POLICY_DENIED));
    }
}
