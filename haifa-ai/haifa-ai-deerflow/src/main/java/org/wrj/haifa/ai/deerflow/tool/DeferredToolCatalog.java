package org.wrj.haifa.ai.deerflow.tool;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.provider.WebFetchProviderRegistry;
import org.wrj.haifa.ai.deerflow.provider.WebSearchProviderRegistry;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.skill.SkillStorage;

@Component
public class DeferredToolCatalog {

    private List<AgentTool> builtinTools;
    private List<Skill> allSkills;
    private ToolRegistry toolRegistry;
    private SkillStorage skillStorage;
    private final WebSearchProviderRegistry searchRegistry;
    private final WebFetchProviderRegistry fetchRegistry;

    @Autowired
    public DeferredToolCatalog(@Lazy ToolRegistry toolRegistry, SkillStorage skillStorage,
            @Autowired(required = false) WebSearchProviderRegistry searchRegistry,
            @Autowired(required = false) WebFetchProviderRegistry fetchRegistry) {
        this.toolRegistry = toolRegistry;
        this.skillStorage = skillStorage;
        this.searchRegistry = searchRegistry;
        this.fetchRegistry = fetchRegistry;
    }

    public DeferredToolCatalog(List<AgentTool> builtinTools, List<Skill> allSkills) {
        this.builtinTools = List.copyOf(builtinTools);
        this.allSkills = List.copyOf(allSkills);
        this.toolRegistry = null;
        this.skillStorage = null;
        this.searchRegistry = null;
        this.fetchRegistry = null;
    }

    private List<AgentTool> builtinTools() {
        if (builtinTools == null) {
            builtinTools = toolRegistry == null ? List.of() : toolRegistry.tools();
        }
        return builtinTools;
    }

    private List<Skill> allSkills() {
        if (allSkills == null) {
            allSkills = skillStorage == null ? List.of() : skillStorage.listAll();
        }
        return allSkills;
    }

    public static String getSource(String toolName) {
        if (toolName.startsWith("mcp__")) {
            return "mcp";
        }
        return switch (toolName) {
            case "present_files", "ask_clarification", "view_image" -> "builtin";
            case "web_search", "web_fetch", "image_search", "ls", "read_file", "glob", "grep", "write_file", "str_replace", "bash", "list_workspace_files", "read_workspace_file", "list_uploaded_files", "read_uploaded_file", "run_script" -> "configured";
            case "task" -> "delegation";
            case "current_time", "tool_search" -> "builtin";
            default -> "configured";
        };
    }

    public static String getCategory(String toolName) {
        if (toolName.startsWith("mcp__")) {
            return "MCP 动态工具";
        }
        return switch (toolName) {
            case "present_files", "ask_clarification", "view_image", "current_time", "tool_search" -> "展示/人机交互";
            case "web_search", "web_fetch", "image_search" -> "Web/检索";
            case "ls", "read_file", "glob", "grep", "read_uploaded_file", "list_uploaded_files", "list_workspace_files", "read_workspace_file" -> "文件读取";
            case "write_file", "str_replace" -> "文件写入/编辑";
            case "bash", "run_script" -> "Shell 执行";
            case "task" -> "子 Agent 委派";
            default -> "展示/人机交互";
        };
    }

    private String buildWebSearchProviderInfo() {
        List<String> allIds = org.wrj.haifa.ai.deerflow.provider.WebSearchProviderType.allIds();
        List<String> registeredIds = searchRegistry != null
                ? searchRegistry.allProviders().stream().map(p -> p.type().id()).toList()
                : List.of(org.wrj.haifa.ai.deerflow.provider.WebSearchProviderType.defaultType().id());
        return "Default: " + org.wrj.haifa.ai.deerflow.provider.WebSearchProviderType.defaultType().id()
                + ". Registered: " + String.join(", ", registeredIds)
                + ". All supported: " + String.join(", ", allIds)
                + (registeredIds.size() < allIds.size() ? " (unregistered are placeholder-only)" : "");
    }

    private String buildWebFetchProviderInfo() {
        List<String> allIds = org.wrj.haifa.ai.deerflow.provider.WebFetchProviderType.allIds();
        List<String> registeredIds = fetchRegistry != null
                ? fetchRegistry.allProviders().stream().map(p -> p.type().id()).toList()
                : List.of(org.wrj.haifa.ai.deerflow.provider.WebFetchProviderType.defaultType().id());
        return "Default: " + org.wrj.haifa.ai.deerflow.provider.WebFetchProviderType.defaultType().id()
                + ". Registered: " + String.join(", ", registeredIds)
                + ". All supported: " + String.join(", ", allIds)
                + (registeredIds.size() < allIds.size() ? " (unregistered are placeholder-only)" : "");
    }

    private List<ToolDescriptor> standardDescriptors() {
        return List.of(
                new ToolDescriptor("present_files", "Present final output files to the user.", "builtin", "展示/人机交互", false),
                new ToolDescriptor("ask_clarification", "Ask the user for structured clarification about ambiguous requirements.", "builtin", "展示/人机交互", false),
                new ToolDescriptor("view_image", "View an image file from the workspace.", "builtin", "展示/人机交互", false),
                new ToolDescriptor("current_time", "Get the current date and time.", "builtin", "展示/人机交互", false),
                new ToolDescriptor("tool_search", "Search available callable tools.", "builtin", "展示/人机交互", false),
                
                new ToolDescriptor("web_search", "Search the web for queries to find relevant sources and snippets.", "configured", "Web/检索", false,
                        buildWebSearchProviderInfo()),
                new ToolDescriptor("web_fetch", "Fetch and read the full markdown content of a URL.", "configured", "Web/检索", false,
                        buildWebFetchProviderInfo()),
                new ToolDescriptor("image_search", "Search the web for images.", "configured", "Web/检索", false),
                
                new ToolDescriptor("ls", "List files and directories in the workspace.", "configured", "文件读取", false),
                new ToolDescriptor("read_file", "Read the content of a file in the workspace.", "configured", "文件读取", false),
                new ToolDescriptor("glob", "Find files in the workspace matching a glob pattern.", "configured", "文件读取", false),
                new ToolDescriptor("grep", "Search for patterns inside workspace files.", "configured", "文件读取", false),
                new ToolDescriptor("list_workspace_files", "Lists files directly under the configured workspace root.", "configured", "文件读取", false),
                new ToolDescriptor("read_workspace_file", "Reads a UTF-8 text file inside the configured workspace root.", "configured", "文件读取", false),
                new ToolDescriptor("list_uploaded_files", "Lists files uploaded by the user.", "configured", "文件读取", false),
                new ToolDescriptor("read_uploaded_file", "Reads the content of an uploaded file.", "configured", "文件读取", false),
                
                new ToolDescriptor("write_file", "Write content to a file in the workspace.", "configured", "文件写入/编辑", false),
                new ToolDescriptor("str_replace", "Find and replace a string in a workspace file.", "configured", "文件写入/编辑", false),
                new ToolDescriptor("bash", "Run a shell command inside the workspace sandbox.", "configured", "Shell 执行", false),
                new ToolDescriptor("run_script", "Generate and execute short scripts (python, powershell, node, bash) for local observation and lightweight tasks.", "configured", "Shell 执行", false),
                
                new ToolDescriptor("task", "Delegate a sub-task to a subagent.", "delegation", "子 Agent 委派", false),
                
                new ToolDescriptor("mcp__placeholder", "Dynamic MCP tool placeholder. Runs MCP server tools.", "mcp", "MCP 动态工具", false)
        );
    }

    public List<ToolDescriptor> search(String keyword) {
        String lower = keyword == null ? "" : keyword.toLowerCase();
        List<ToolDescriptor> results = new java.util.ArrayList<>();
        for (ToolDescriptor td : standardDescriptors()) {
            if (matches(td.name(), td.description(), lower)) {
                results.add(td);
            }
        }
        return results;
    }

    public List<ToolDescriptor> listAll() {
        return standardDescriptors();
    }

    private static boolean matches(String name, String description, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        String n = name == null ? "" : name.toLowerCase();
        String d = description == null ? "" : description.toLowerCase();
        return n.contains(keyword) || d.contains(keyword);
    }
}
