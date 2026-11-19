package org.wrj.haifa.ai.utilitymcp.provider;

import java.util.Map;

@FunctionalInterface
public interface JsonProvider {
    ProviderPayload get(String path, Map<String, ?> query);
}
