package org.wrj.haifa.ai.deerflow.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactService;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

import static org.assertj.core.api.Assertions.assertThat;

class SafetyToolsTest {

    @Test
    void writeFileEnforcesBoundariesAndToggle(@TempDir Path tmp) throws IOException {
        Path workspace = tmp.resolve("workspace");
        Path uploads = tmp.resolve("uploads");
        Path outputs = tmp.resolve("outputs");
        Files.createDirectories(workspace);
        Files.createDirectories(uploads);
        Files.createDirectories(outputs);

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(workspace.toString());
        properties.setUploadsRoot(uploads.toString());
        properties.setOutputsRoot(outputs.toString());
        properties.setWriteFileEnabled(true);

        ArtifactService artifactService = new ArtifactService(properties);
        WriteFileTool tool = new WriteFileTool(properties, artifactService);

        // 1. Success write inside workspace
        ToolRequest req1 = new ToolRequest("{\"path\": \"foo.txt\", \"content\": \"hello\"}", workspace);
        ToolResult res1 = tool.execute(req1);
        assertThat(res1.content()).contains("written successfully");
        assertThat(Files.readString(workspace.resolve("foo.txt"))).isEqualTo("hello");

        // 2. Success write inside outputsRoot
        ToolRequest req2 = new ToolRequest("{\"path\": \"../outputs/bar.txt\", \"content\": \"output\"}",
                workspace, List.of(), "thread-1", "run-1");
        ToolResult res2 = tool.execute(req2);
        assertThat(res2.content()).contains("registered for download", "/api/deerflow/artifacts/");
        assertThat(res2.metadata()).containsKeys("artifactId", "downloadUrl");
        assertThat(artifactService.list("thread-1", "run-1")).hasSize(1);
        assertThat(Files.readString(outputs.resolve("bar.txt"))).isEqualTo("output");

        // 3. Frontend uploads are readable but not writable by agent file tools
        ToolRequest uploadsReq = new ToolRequest("{\"path\": \"/mnt/user-data/uploads/agent.txt\", \"content\": \"blocked\"}", workspace);
        ToolResult uploadsRes = tool.execute(uploadsReq);
        assertThat(uploadsRes.content()).contains("Security Exception", "uploads directory is written only by the frontend upload service");

        // 4. Security block when writing outside allowed roots
        ToolRequest req3 = new ToolRequest("{\"path\": \"../../outside.txt\", \"content\": \"escaped\"}", workspace);
        ToolResult res3 = tool.execute(req3);
        assertThat(res3.content()).contains("Security Exception");

        // 5. Toggle disabled block
        properties.setWriteFileEnabled(false);
        ToolResult res4 = tool.execute(req1);
        assertThat(res4.content()).contains("disabled by security configuration");
    }

    @Test
    void strReplaceEnforcesBoundariesAndToggle(@TempDir Path tmp) throws IOException {
        Path workspace = tmp.resolve("workspace");
        Files.createDirectories(workspace);
        Files.writeString(workspace.resolve("foo.txt"), "hello world");

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(workspace.toString());
        properties.setStrReplaceEnabled(true);

        StrReplaceTool tool = new StrReplaceTool(properties);

        // 1. Success replace
        ToolRequest req1 = new ToolRequest("{\"path\": \"foo.txt\", \"old_str\": \"world\", \"new_str\": \"deerflow\"}", workspace);
        ToolResult res1 = tool.execute(req1);
        assertThat(res1.content()).contains("replaced successfully");
        assertThat(Files.readString(workspace.resolve("foo.txt"))).isEqualTo("hello deerflow");

        // 2. String not found error
        ToolRequest req2 = new ToolRequest("{\"path\": \"foo.txt\", \"old_str\": \"notexist\", \"new_str\": \"abc\"}", workspace);
        ToolResult res2 = tool.execute(req2);
        assertThat(res2.content()).contains("old_str not found");

        // 3. Security block outside roots
        ToolRequest req3 = new ToolRequest("{\"path\": \"../../outside.txt\", \"old_str\": \"a\", \"new_str\": \"b\"}", workspace);
        ToolResult res3 = tool.execute(req3);
        assertThat(res3.content()).contains("Security Exception");

        // 4. Toggle disabled block
        properties.setStrReplaceEnabled(false);
        ToolResult res4 = tool.execute(req1);
        assertThat(res4.content()).contains("disabled by security configuration");
    }

    @Test
    void bashToolEnforcesSecurityToggle(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setBashEnabled(false);

        BashTool tool = new BashTool(properties);
        ToolRequest req = new ToolRequest("{\"command\": \"echo hello\"}", tmp);
        ToolResult res = tool.execute(req);
        assertThat(res.content()).contains("disabled by security configuration");
    }

    @Test
    void readFileAndLsAllowSkillsRoot(@TempDir Path tmp) throws IOException {
        Path workspace = tmp.resolve("workspace");
        Path skills = tmp.resolve("skills");
        Files.createDirectories(workspace);
        Files.createDirectories(skills);
        Files.writeString(skills.resolve("SKILL.md"), "skill content");

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(workspace.toString());
        properties.setSkillsRoot(skills.toString());

        ReadFileTool readTool = new ReadFileTool(properties);
        LsTool lsTool = new LsTool(properties);

        // 1. Read file inside skillsRoot succeeds
        ToolRequest readReq = new ToolRequest("{\"path\": \"../skills/SKILL.md\"}", workspace);
        ToolResult readRes = readTool.execute(readReq);
        assertThat(readRes.content()).isEqualTo("skill content");

        // 2. Ls inside skillsRoot succeeds
        ToolRequest lsReq = new ToolRequest("{\"path\": \"../skills\"}", workspace);
        ToolResult lsRes = lsTool.execute(lsReq);
        assertThat(lsRes.content()).contains("SKILL.md");

        // 3. Out-of-bounds still throws Security Exception
        ToolRequest badReq = new ToolRequest("{\"path\": \"../../outside.md\"}", workspace);
        ToolResult badRes = readTool.execute(badReq);
        assertThat(badRes.content()).contains("Security Exception");
    }
}
