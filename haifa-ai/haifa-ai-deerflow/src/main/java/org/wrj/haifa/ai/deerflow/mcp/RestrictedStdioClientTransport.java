package org.wrj.haifa.ai.deerflow.mcp;

import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import java.io.File;

final class RestrictedStdioClientTransport extends StdioClientTransport {

    private final File workingDirectory;

    RestrictedStdioClientTransport(ServerParameters parameters, McpJsonMapper jsonMapper, File workingDirectory) {
        super(parameters, jsonMapper);
        this.workingDirectory = workingDirectory;
    }

    @Override
    protected ProcessBuilder getProcessBuilder() {
        ProcessBuilder builder = new ProcessBuilder();
        builder.environment().clear();
        if (workingDirectory != null) builder.directory(workingDirectory);
        return builder;
    }
}
