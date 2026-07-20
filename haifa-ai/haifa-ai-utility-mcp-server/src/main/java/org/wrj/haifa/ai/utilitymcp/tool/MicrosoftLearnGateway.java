package org.wrj.haifa.ai.utilitymcp.tool;

import java.util.Map;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityResult;

public interface MicrosoftLearnGateway {

    UtilityResult call(String toolName, Map<String, Object> arguments);
}
