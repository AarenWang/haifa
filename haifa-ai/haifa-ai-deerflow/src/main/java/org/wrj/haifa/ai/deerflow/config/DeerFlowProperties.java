package org.wrj.haifa.ai.deerflow.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.provider.WebFetchProviderType;
import org.wrj.haifa.ai.deerflow.provider.WebSearchProviderType;

@ConfigurationProperties(prefix = "haifa.ai.deerflow")
public class DeerFlowProperties {

    private String model;
    private String systemPrompt;
    private String workspaceRoot = ".";
    private String skillsRoot = ".";
    private boolean skillsEnabled = true;
    private boolean mcpEnabled = false;
    private boolean toolSearchEnabled = true;

    @Min(1_000)
    private long modelTimeout = 300_000; // 5 minutes in ms

    private String uploadsRoot = "${user.dir}/uploads";
    private String outputsRoot = "${user.dir}/outputs";
    private boolean writeFileEnabled = true;
    private boolean strReplaceEnabled = true;
    private boolean bashEnabled = false;
    private boolean runScriptEnabled = false;
    private Sandbox sandbox = new Sandbox();
    private Graph graph = new Graph();
    private Approval approval = new Approval();
    private long maxUploadBytes = 10_485_760;
    private int maxConvertedChars = 60_000;
    private String allowedUploadExtensions = "txt,md,json,csv,log,xml,yml,yaml,properties";

    @Min(1)
    @Max(20)
    private int maxIterations = 4;

    @Min(0)
    private int charBudget = 0;

    // Research mode configuration
    @Min(1)
    @Max(100)
    private int maxResearchSteps = 20;

    @Min(1)
    @Max(50)
    private int maxResearchSources = 20;

    @Min(1)
    @Max(30)
    private int maxFetchesPerRun = 10;

    @Min(10_000)
    private long researchTimeout = 300_000; // 5 minutes in ms

    private String defaultResearchDepth = "STANDARD";

    private boolean researchEnabled = true;
    private String researchSystemPrompt;

    // Persistence configuration
    private Persistence persistence = new Persistence();

    // Tool provider configuration
    private Tools tools = new Tools();

    public static class Persistence {
        private Sqlite sqlite = new Sqlite();

        public Sqlite getSqlite() {
            return sqlite;
        }

        public void setSqlite(Sqlite sqlite) {
            this.sqlite = sqlite;
        }
    }

    public static class Sqlite {
        private String path = "${user.dir}/data/deerflow.sqlite";

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class Tools {
        private WebSearchToolConfig webSearch = new WebSearchToolConfig();
        private WebFetchToolConfig webFetch = new WebFetchToolConfig();

        public WebSearchToolConfig getWebSearch() {
            return webSearch;
        }

        public void setWebSearch(WebSearchToolConfig webSearch) {
            this.webSearch = webSearch;
        }

        public WebFetchToolConfig getWebFetch() {
            return webFetch;
        }

        public void setWebFetch(WebFetchToolConfig webFetch) {
            this.webFetch = webFetch;
        }
    }

    public static class Sandbox {
        private boolean enabled = false;
        private String backend = "local";
        private String dockerImage = "ubuntu:24.04";
        private boolean networkEnabled = false;
        private long timeoutMs = 30_000;
        private int maxOutputChars = 20_000;
        private String workdirSubdir = "sandbox";
        private String allowedCommands = "mvn,npm,node,python,python3,java,javac,ls,pwd,cat,rg,grep";
        private String deniedPatterns = "rm -rf,format,shutdown,reboot,del /s,Remove-Item -Recurse";
        private String allowedScriptLanguages = "python,powershell,bash";
        private String scriptWorkdirSubdir = "scripts";
        private boolean runScriptLocalUnsafeAllowed = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBackend() {
            return backend;
        }

        public void setBackend(String backend) {
            this.backend = backend;
        }

        public String getDockerImage() {
            return dockerImage;
        }

        public void setDockerImage(String dockerImage) {
            this.dockerImage = dockerImage;
        }

        public boolean isNetworkEnabled() {
            return networkEnabled;
        }

        public void setNetworkEnabled(boolean networkEnabled) {
            this.networkEnabled = networkEnabled;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getMaxOutputChars() {
            return maxOutputChars;
        }

        public void setMaxOutputChars(int maxOutputChars) {
            this.maxOutputChars = maxOutputChars;
        }

        public String getWorkdirSubdir() {
            return workdirSubdir;
        }

        public void setWorkdirSubdir(String workdirSubdir) {
            this.workdirSubdir = workdirSubdir;
        }

        public String getAllowedCommands() {
            return allowedCommands;
        }

        public void setAllowedCommands(String allowedCommands) {
            this.allowedCommands = allowedCommands;
        }

        public String getDeniedPatterns() {
            return deniedPatterns;
        }

        public void setDeniedPatterns(String deniedPatterns) {
            this.deniedPatterns = deniedPatterns;
        }

        public String getAllowedScriptLanguages() {
            return allowedScriptLanguages;
        }

        public void setAllowedScriptLanguages(String allowedScriptLanguages) {
            this.allowedScriptLanguages = allowedScriptLanguages;
        }

        public String getScriptWorkdirSubdir() {
            return scriptWorkdirSubdir;
        }

        public void setScriptWorkdirSubdir(String scriptWorkdirSubdir) {
            this.scriptWorkdirSubdir = scriptWorkdirSubdir;
        }

        public boolean isRunScriptLocalUnsafeAllowed() {
            return runScriptLocalUnsafeAllowed;
        }

        public void setRunScriptLocalUnsafeAllowed(boolean runScriptLocalUnsafeAllowed) {
            this.runScriptLocalUnsafeAllowed = runScriptLocalUnsafeAllowed;
        }
    }

    public static class Graph {
        private boolean enabled = false;
        private GraphRuntimeMode mode = GraphRuntimeMode.OFF;
        private Checkpoint checkpoint = new Checkpoint();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public GraphRuntimeMode getMode() {
            return mode;
        }

        public void setMode(GraphRuntimeMode mode) {
            this.mode = mode == null ? GraphRuntimeMode.OFF : mode;
        }

        public Checkpoint getCheckpoint() {
            return checkpoint;
        }

        public void setCheckpoint(Checkpoint checkpoint) {
            this.checkpoint = checkpoint == null ? new Checkpoint() : checkpoint;
        }
    }

    public static class Checkpoint {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class WebSearchToolConfig {
        private String provider = WebSearchProviderType.defaultType().id();
        private String apiKey;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class WebFetchToolConfig {
        private String provider = WebFetchProviderType.defaultType().id();
        private String apiKey;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
    }

    public Tools getTools() {
        return tools;
    }

    public void setTools(Tools tools) {
        this.tools = tools;
    }

    public String getWebSearchProvider() {
        return tools != null && tools.webSearch != null ? tools.webSearch.getProvider() : WebSearchProviderType.defaultType().id();
    }

    public String getWebFetchProvider() {
        return tools != null && tools.webFetch != null ? tools.webFetch.getProvider() : WebFetchProviderType.defaultType().id();
    }

    public String getModel() {
        return model;
    }

    public String getResearchSystemPrompt() {
        return researchSystemPrompt;
    }

    public void setResearchSystemPrompt(String researchSystemPrompt) {
        this.researchSystemPrompt = researchSystemPrompt;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getWorkspaceRoot() {
        Path configured = resolvePath(workspaceRoot, ".");
        if (!StringUtils.hasText(System.getenv("HAIFA_DEERFLOW_WORKSPACE"))
                && looksLikeDeerFlowModule(configured)) {
            return discoverRepositoryRoot(configured).toString();
        }
        return configured.toString();
    }

    public void setWorkspaceRoot(String workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    public String getSkillsRoot() {
        return skillsRoot;
    }

    public void setSkillsRoot(String skillsRoot) {
        this.skillsRoot = skillsRoot;
    }

    public boolean isSkillsEnabled() {
        return skillsEnabled;
    }

    public void setSkillsEnabled(boolean skillsEnabled) {
        this.skillsEnabled = skillsEnabled;
    }

    public boolean isMcpEnabled() {
        return mcpEnabled;
    }

    public void setMcpEnabled(boolean mcpEnabled) {
        this.mcpEnabled = mcpEnabled;
    }

    public boolean isToolSearchEnabled() {
        return toolSearchEnabled;
    }

    public void setToolSearchEnabled(boolean toolSearchEnabled) {
        this.toolSearchEnabled = toolSearchEnabled;
    }

    public long getModelTimeout() {
        return modelTimeout;
    }

    public void setModelTimeout(long modelTimeout) {
        this.modelTimeout = modelTimeout;
    }

    public String getUploadsRoot() {
        return uploadsRoot;
    }

    public void setUploadsRoot(String uploadsRoot) {
        this.uploadsRoot = uploadsRoot;
    }

    public long getMaxUploadBytes() {
        return maxUploadBytes;
    }

    public void setMaxUploadBytes(long maxUploadBytes) {
        this.maxUploadBytes = maxUploadBytes;
    }

    public int getMaxConvertedChars() {
        return maxConvertedChars;
    }

    public void setMaxConvertedChars(int maxConvertedChars) {
        this.maxConvertedChars = maxConvertedChars;
    }

    public String getAllowedUploadExtensions() {
        return allowedUploadExtensions;
    }

    public void setAllowedUploadExtensions(String allowedUploadExtensions) {
        this.allowedUploadExtensions = allowedUploadExtensions;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public int getCharBudget() {
        return charBudget;
    }

    public void setCharBudget(int charBudget) {
        this.charBudget = charBudget;
    }

    // Research getters/setters
    public int getMaxResearchSteps() {
        return maxResearchSteps;
    }

    public void setMaxResearchSteps(int maxResearchSteps) {
        this.maxResearchSteps = maxResearchSteps;
    }

    public int getMaxResearchSources() {
        return maxResearchSources;
    }

    public void setMaxResearchSources(int maxResearchSources) {
        this.maxResearchSources = maxResearchSources;
    }

    public int getMaxFetchesPerRun() {
        return maxFetchesPerRun;
    }

    public void setMaxFetchesPerRun(int maxFetchesPerRun) {
        this.maxFetchesPerRun = maxFetchesPerRun;
    }

    public long getResearchTimeout() {
        return researchTimeout;
    }

    public void setResearchTimeout(long researchTimeout) {
        this.researchTimeout = researchTimeout;
    }

    public String getDefaultResearchDepth() {
        return defaultResearchDepth;
    }

    public void setDefaultResearchDepth(String defaultResearchDepth) {
        this.defaultResearchDepth = defaultResearchDepth;
    }

    public boolean isResearchEnabled() {
        return researchEnabled;
    }

    public void setResearchEnabled(boolean researchEnabled) {
        this.researchEnabled = researchEnabled;
    }

    // Tool output budget configuration (inspired by Python deer-flow ToolOutputConfig)
    private ToolOutputBudget toolOutputBudget = new ToolOutputBudget();

    // Summarization configuration (inspired by Python deer-flow SummarizationConfig)
    private Summarization summarization = new Summarization();

    public static class ToolOutputBudget {
        private boolean enabled = true;
        private boolean externalizeEnabled = true;
        private int externalizeMinChars = 10_000;
        private int fallbackMaxChars = 8_000;
        private int fallbackHeadChars = 1_500;
        private int fallbackTailChars = 1_500;
        private int previewHeadChars = 1_500;
        private int previewTailChars = 1_500;
        private String storageSubdir = "tool-outputs";
        private String exemptTools = "present_files,view_image";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isExternalizeEnabled() { return externalizeEnabled; }
        public void setExternalizeEnabled(boolean externalizeEnabled) { this.externalizeEnabled = externalizeEnabled; }
        public int getExternalizeMinChars() { return externalizeMinChars; }
        public void setExternalizeMinChars(int externalizeMinChars) { this.externalizeMinChars = externalizeMinChars; }
        public int getFallbackMaxChars() { return fallbackMaxChars; }
        public void setFallbackMaxChars(int fallbackMaxChars) { this.fallbackMaxChars = fallbackMaxChars; }
        public int getFallbackHeadChars() { return fallbackHeadChars; }
        public void setFallbackHeadChars(int fallbackHeadChars) { this.fallbackHeadChars = fallbackHeadChars; }
        public int getFallbackTailChars() { return fallbackTailChars; }
        public void setFallbackTailChars(int fallbackTailChars) { this.fallbackTailChars = fallbackTailChars; }
        public int getPreviewHeadChars() { return previewHeadChars; }
        public void setPreviewHeadChars(int previewHeadChars) { this.previewHeadChars = previewHeadChars; }
        public int getPreviewTailChars() { return previewTailChars; }
        public void setPreviewTailChars(int previewTailChars) { this.previewTailChars = previewTailChars; }
        public String getStorageSubdir() { return storageSubdir; }
        public void setStorageSubdir(String storageSubdir) { this.storageSubdir = storageSubdir; }
        public String getExemptTools() { return exemptTools; }
        public void setExemptTools(String exemptTools) { this.exemptTools = exemptTools; }
    }

    public static class Summarization {
        private boolean enabled = false;
        private String summaryModelName; // lightweight model for summarization (e.g. gpt-4o-mini)
        private int triggerMessages = 10;
        private int triggerChars = 8_000;
        private int keepMessages = 4;
        private int trimTokensToSummarize = 4_000;
        private int preserveRecentSkillCount = 5;
        private int preserveRecentSkillTokens = 25_000;
        private int preserveRecentSkillTokensPerSkill = 5_000;
        private String skillFileReadToolNames = "read_file,read,view,cat";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getSummaryModelName() { return summaryModelName; }
        public void setSummaryModelName(String summaryModelName) { this.summaryModelName = summaryModelName; }
        public int getTriggerMessages() { return triggerMessages; }
        public void setTriggerMessages(int triggerMessages) { this.triggerMessages = triggerMessages; }
        public int getTriggerChars() { return triggerChars; }
        public void setTriggerChars(int triggerChars) { this.triggerChars = triggerChars; }
        public int getKeepMessages() { return keepMessages; }
        public void setKeepMessages(int keepMessages) { this.keepMessages = keepMessages; }
        public int getTrimTokensToSummarize() { return trimTokensToSummarize; }
        public void setTrimTokensToSummarize(int trimTokensToSummarize) { this.trimTokensToSummarize = trimTokensToSummarize; }
        public int getPreserveRecentSkillCount() { return preserveRecentSkillCount; }
        public void setPreserveRecentSkillCount(int preserveRecentSkillCount) { this.preserveRecentSkillCount = preserveRecentSkillCount; }
        public int getPreserveRecentSkillTokens() { return preserveRecentSkillTokens; }
        public void setPreserveRecentSkillTokens(int preserveRecentSkillTokens) { this.preserveRecentSkillTokens = preserveRecentSkillTokens; }
        public int getPreserveRecentSkillTokensPerSkill() { return preserveRecentSkillTokensPerSkill; }
        public void setPreserveRecentSkillTokensPerSkill(int preserveRecentSkillTokensPerSkill) { this.preserveRecentSkillTokensPerSkill = preserveRecentSkillTokensPerSkill; }
        public String getSkillFileReadToolNames() { return skillFileReadToolNames; }
        public void setSkillFileReadToolNames(String skillFileReadToolNames) { this.skillFileReadToolNames = skillFileReadToolNames; }
    }

    public ToolOutputBudget getToolOutputBudget() { return toolOutputBudget; }
    public void setToolOutputBudget(ToolOutputBudget toolOutputBudget) { this.toolOutputBudget = toolOutputBudget; }
    public Summarization getSummarization() { return summarization; }
    public void setSummarization(Summarization summarization) { this.summarization = summarization; }

    public String getOutputsRoot() {
        if (usesDefaultOutputsRoot()) {
            return Path.of(getWorkspaceRoot()).resolve("outputs").toAbsolutePath().normalize().toString();
        }
        return resolvePath(outputsRoot, "outputs").toString();
    }

    public void setOutputsRoot(String outputsRoot) {
        this.outputsRoot = outputsRoot;
    }

    private boolean usesDefaultOutputsRoot() {
        if (StringUtils.hasText(System.getenv("HAIFA_DEERFLOW_OUTPUTS"))) {
            return false;
        }
        if (!StringUtils.hasText(outputsRoot)) {
            return true;
        }
        String normalized = outputsRoot.replace('\\', '/').toLowerCase(Locale.ROOT);
        String userDirOutputs = Path.of(System.getProperty("user.dir"), "outputs")
                .toAbsolutePath().normalize().toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.equals("${user.dir}/outputs")
                || resolvePath(outputsRoot, "outputs").toString().replace('\\', '/').toLowerCase(Locale.ROOT)
                        .equals(userDirOutputs);
    }

    private static Path resolvePath(String configured, String fallback) {
        String value = StringUtils.hasText(configured) ? configured : fallback;
        value = value.replace("${user.dir}", System.getProperty("user.dir"));
        return Path.of(value).toAbsolutePath().normalize();
    }

    private static boolean looksLikeDeerFlowModule(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && "haifa-ai-deerflow".equalsIgnoreCase(fileName.toString())
                && Files.isRegularFile(path.resolve("pom.xml"));
    }

    private static Path discoverRepositoryRoot(Path start) {
        Path current = start.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("AGENTS.md"))
                    && Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        return start.toAbsolutePath().normalize();
    }

    public boolean isWriteFileEnabled() {
        return writeFileEnabled;
    }

    public void setWriteFileEnabled(boolean writeFileEnabled) {
        this.writeFileEnabled = writeFileEnabled;
    }

    public boolean isStrReplaceEnabled() {
        return strReplaceEnabled;
    }

    public void setStrReplaceEnabled(boolean strReplaceEnabled) {
        this.strReplaceEnabled = strReplaceEnabled;
    }

    public boolean isBashEnabled() {
        return bashEnabled;
    }

    public void setBashEnabled(boolean bashEnabled) {
        this.bashEnabled = bashEnabled;
    }

    public boolean isRunScriptEnabled() {
        return runScriptEnabled;
    }

    public void setRunScriptEnabled(boolean runScriptEnabled) {
        this.runScriptEnabled = runScriptEnabled;
    }

    public Sandbox getSandbox() {
        return sandbox;
    }

    public void setSandbox(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph == null ? new Graph() : graph;
    }

    // Subagent configuration
    private int subagentMaxPerResponse = 3;
    private int subagentMaxConcurrent = 3;

    public int getSubagentMaxPerResponse() {
        return subagentMaxPerResponse;
    }

    public void setSubagentMaxPerResponse(int subagentMaxPerResponse) {
        this.subagentMaxPerResponse = Math.max(1, subagentMaxPerResponse);
    }

    public int getSubagentMaxConcurrent() {
        return subagentMaxConcurrent;
    }

    public void setSubagentMaxConcurrent(int subagentMaxConcurrent) {
        this.subagentMaxConcurrent = Math.max(1, subagentMaxConcurrent);
    }

    public Approval getApproval() {
        return approval;
    }

    public void setApproval(Approval approval) {
        this.approval = approval;
    }

    public static class Approval {
        private boolean enabled = true;
        private int defaultTimeoutSeconds = 120;
        private boolean allowSessionApproval = true;
        private boolean allowAlwaysApproval = false;
        private boolean denyOnTimeout = true;
        private boolean requireForLocalScript = true;
        private boolean requireForNetwork = true;
        private boolean requireForFileWrite = true;
        private boolean hardlinePatternsEnabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getDefaultTimeoutSeconds() { return defaultTimeoutSeconds; }
        public void setDefaultTimeoutSeconds(int defaultTimeoutSeconds) { this.defaultTimeoutSeconds = defaultTimeoutSeconds; }

        public boolean isAllowSessionApproval() { return allowSessionApproval; }
        public void setAllowSessionApproval(boolean allowSessionApproval) { this.allowSessionApproval = allowSessionApproval; }

        public boolean isAllowAlwaysApproval() { return allowAlwaysApproval; }
        public void setAllowAlwaysApproval(boolean allowAlwaysApproval) { this.allowAlwaysApproval = allowAlwaysApproval; }

        public boolean isDenyOnTimeout() { return denyOnTimeout; }
        public void setDenyOnTimeout(boolean denyOnTimeout) { this.denyOnTimeout = denyOnTimeout; }

        public boolean isRequireForLocalScript() { return requireForLocalScript; }
        public void setRequireForLocalScript(boolean requireForLocalScript) { this.requireForLocalScript = requireForLocalScript; }

        public boolean isRequireForNetwork() { return requireForNetwork; }
        public void setRequireForNetwork(boolean requireForNetwork) { this.requireForNetwork = requireForNetwork; }

        public boolean isRequireForFileWrite() { return requireForFileWrite; }
        public void setRequireForFileWrite(boolean requireForFileWrite) { this.requireForFileWrite = requireForFileWrite; }

        public boolean isHardlinePatternsEnabled() { return hardlinePatternsEnabled; }
        public void setHardlinePatternsEnabled(boolean hardlinePatternsEnabled) { this.hardlinePatternsEnabled = hardlinePatternsEnabled; }
    }
}
