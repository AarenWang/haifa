package org.wrj.haifa.ai.spring.toolcalling;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MimeTypeUtils;
import org.wrj.haifa.ai.spring.toolcalling.system.CommandResult;
import org.wrj.haifa.ai.spring.toolcalling.system.PlannedCommand;
import org.wrj.haifa.ai.spring.toolcalling.system.SystemCommandPlanner;
import org.wrj.haifa.ai.spring.toolcalling.system.SystemCommandRunner;

@SpringBootTest
@AutoConfigureMockMvc
class SystemInfoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void chatEndpointSummarizesLocalCommands() throws Exception {
        mockMvc.perform(post("/api/system/chat")
                .contentType(MimeTypeUtils.APPLICATION_JSON_VALUE)
                .content("{\"message\":\"cpu memory disk\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("cpu info")))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("memory info")))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("disk info")))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("CPU overview")))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("via `lscpu")));
    }

    @TestConfiguration
    static class StubCommandRunnerConfiguration {

        @Bean
        @Primary
        SystemCommandRunner stubSystemCommandRunner() {
            return (description, command) -> {
                String stdout = switch (description) {
                    case "CPU inspection" -> "cpu info";
                    case "Memory inspection" -> "memory info";
                    case "Process inspection" -> "process info";
                    case "Network port inspection" -> "port info";
                    case "Disk inspection" -> "disk info";
                    default -> "";
                };
                return new CommandResult(description, String.join(" ", command), stdout, "", 0);
            };
        }

        @Bean
        @Primary
        SystemCommandPlanner stubSystemCommandPlanner() {
            return (instruction, directive) -> new PlannedCommand(directive.defaultCommand(), "stub");
        }
    }
}
