package org.wrj.haifa.ai.utilitymcp.tool;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.wrj.haifa.ai.utilitymcp.mcp.ToolArguments;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityErrorCode;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityResult;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

/** Stable Utility MCP contracts backed by the official Microsoft Learn MCP server. */
public class MicrosoftLearnService {

    private final MicrosoftLearnGateway gateway;

    public MicrosoftLearnService(MicrosoftLearnGateway gateway) {
        this.gateway = gateway;
    }

    public UtilityResult searchDocs(Map<String, Object> arguments) {
        String query = new ToolArguments(arguments).requiredString("query", 1_000);
        return gateway.call("microsoft_docs_search", Map.of("query", query));
    }

    public UtilityResult fetchDocs(Map<String, Object> arguments) {
        String url = new ToolArguments(arguments).requiredString("url", 2_048);
        validateMicrosoftLearnUrl(url);
        return gateway.call("microsoft_docs_fetch", Map.of("url", url));
    }

    public UtilityResult searchCodeSamples(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        String query = args.requiredString("query", 1_000);
        String language = args.optionalString("language", null, 64);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("query", query);
        if (language != null) request.put("language", language);
        return gateway.call("microsoft_code_sample_search", Map.copyOf(request));
    }

    private static void validateMicrosoftLearnUrl(String raw) {
        try {
            URI uri = URI.create(raw);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null
                    || !(host.equalsIgnoreCase("learn.microsoft.com") || host.toLowerCase(java.util.Locale.ROOT).endsWith(".learn.microsoft.com"))) {
                throw new UtilityToolException(UtilityErrorCode.POLICY_DENIED,
                        "url must be an HTTPS Microsoft Learn documentation URL", false);
            }
        }
        catch (UtilityToolException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            throw UtilityToolException.invalid("url must be a valid HTTPS Microsoft Learn documentation URL");
        }
    }
}
