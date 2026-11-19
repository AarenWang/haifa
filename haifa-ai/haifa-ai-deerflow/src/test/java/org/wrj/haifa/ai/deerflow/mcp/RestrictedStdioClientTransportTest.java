package org.wrj.haifa.ai.deerflow.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RestrictedStdioClientTransportTest {

    @TempDir
    Path workingDirectory;

    @Test
    void startsWithEmptyEnvironmentAndControlledWorkingDirectory() {
        RestrictedStdioClientTransport transport = new RestrictedStdioClientTransport(
                ServerParameters.builder("java").args("-version").env(java.util.Map.of()).build(),
                new JacksonMcpJsonMapper(new ObjectMapper()), workingDirectory.toFile());

        ProcessBuilder builder = transport.getProcessBuilder();

        assertThat(builder.environment()).isEmpty();
        assertThat(builder.directory()).isEqualTo(workingDirectory.toFile());
    }
}
