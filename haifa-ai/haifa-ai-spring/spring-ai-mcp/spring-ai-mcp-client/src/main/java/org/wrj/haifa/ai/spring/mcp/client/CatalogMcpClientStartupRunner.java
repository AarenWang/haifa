package org.wrj.haifa.ai.spring.mcp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.spring.mcp.client.service.CatalogMcpClientService;

@Component
public class CatalogMcpClientStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogMcpClientStartupRunner.class);

    private final CatalogMcpClientService catalogMcpClientService;

    public CatalogMcpClientStartupRunner(CatalogMcpClientService catalogMcpClientService) {
        this.catalogMcpClientService = catalogMcpClientService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Connected MCP overview: {}", catalogMcpClientService.overview());
        log.info("MCP example 1 - tool call: {}", catalogMcpClientService.runExampleToolCall());
        log.info("MCP example 2 - resource and prompt: {}", catalogMcpClientService.runExampleResourceAndPrompt());
    }
}
