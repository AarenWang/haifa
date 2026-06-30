package org.wrj.haifa.ai.deerflow.webcontent;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.research.SourceRegistry;

@Component
public class FetchPolicy {

    private final SourceRegistry sourceRegistry;

    public FetchPolicy(SourceRegistry sourceRegistry) {
        this.sourceRegistry = sourceRegistry;
    }

    public boolean shouldFetch(String url) {
        return this.sourceRegistry.findFetchedByUrl(url).isEmpty();
    }
}
