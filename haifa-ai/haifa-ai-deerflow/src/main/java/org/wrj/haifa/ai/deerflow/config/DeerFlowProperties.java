package org.wrj.haifa.ai.deerflow.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.provider.WebFetchProviderType;
import org.wrj.haifa.ai.deerflow.provider.WebSearchProviderType;
import org.wrj.haifa.ai.deerflow.run.RunConcurrencyMode;

@ConfigurationProperties(prefix = "haifa.ai.deerflow")
public class DeerFlowProperties {

    private String model;
    private String networkProxyUrl;
    private String networkProxyUsername;
    private String networkProxyPassword;
    private String systemPrompt;
    private String userDataRoot = "${user.dir}/data/user-data";
    private String workspaceRoot = "${user.dir}/data/user-data/workspace";
    private String skillsRoot = ".";
    private String skillsContainerPath = "/mnt/skills";
    private boolean skillsEnabled = true;
    private boolean mcpEnabled = false;
    private Mcp mcp = new Mcp();
    private boolean toolSearchEnabled = true;
    /** Phase-62 compatibility default; production choice is made at manual gate 2. */
    private RunConcurrencyMode runConcurrencyMode = RunConcurrencyMode.ALLOW_PARALLEL_RUNS;

    @Min(1_000)
    private long modelTimeout = 300_000; // 5 minutes in ms

    private String uploadsRoot = "${user.dir}/data/user-data/uploads";
    private String outputsRoot = "${user.dir}/data/user-data/outputs";
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

    @Min(1)
    @Max(100)
    private int maxToolCalls = 80;

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

    public static class Mcp {
        private boolean enabled = false;
        private int directExposureThreshold = 20;
        private Map<String, String> capabilityOwners = new LinkedHashMap<>();
        private Map<String, McpServer> servers = new LinkedHashMap<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getDirectExposureThreshold() { return directExposureThreshold; }
        public void setDirectExposureThreshold(int directExposureThreshold) {
            this.directExposureThreshold = Math.max(1, directExposureThreshold);
        }
        public Map<String, String> getCapabilityOwners() { return capabilityOwners; }
        public void setCapabilityOwners(Map<String, String> capabilityOwners) {
            this.capabilityOwners = capabilityOwners == null ? new LinkedHashMap<>() : new LinkedHashMap<>(capabilityOwners);
        }
        public Map<String, McpServer> getServers() { return servers; }
        public void setServers(Map<String, McpServer> servers) {
            this.servers = servers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(servers);
        }
    }

    public static class McpServer {
        private boolean enabled;
        private boolean required;
        private String transport = "STREAMABLE_HTTP";
        private String url = "";
        private String endpoint = "/mcp";
        private String command = "";
        private java.util.List<String> args = new java.util.ArrayList<>();
        private String cwd = "";
        private java.util.List<String> environmentNames = new java.util.ArrayList<>();
        private String bearerTokenEnvironment = "";
        private String origin = "";
        private java.util.List<String> allowedTools = new java.util.ArrayList<>();
        private java.util.List<String> deniedTools = new java.util.ArrayList<>();
        private String defaultRisk = "UNKNOWN";
        private String stalePolicy = "DENY_NEW_CALLS";
        private long requestTimeoutMs = 15_000;
        private Map<String, String> semanticMappings = new LinkedHashMap<>();
        private Map<String, String> capabilityMappings = new LinkedHashMap<>();
        private Map<String, String> riskMappings = new LinkedHashMap<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        public String getTransport() { return transport; }
        public void setTransport(String transport) { this.transport = transport; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public java.util.List<String> getArgs() { return args; }
        public void setArgs(java.util.List<String> args) { this.args = args == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(args); }
        public String getCwd() { return cwd; }
        public void setCwd(String cwd) { this.cwd = cwd; }
        public java.util.List<String> getEnvironmentNames() { return environmentNames; }
        public void setEnvironmentNames(java.util.List<String> environmentNames) {
            this.environmentNames = environmentNames == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(environmentNames);
        }
        public String getBearerTokenEnvironment() { return bearerTokenEnvironment; }
        public void setBearerTokenEnvironment(String bearerTokenEnvironment) { this.bearerTokenEnvironment = bearerTokenEnvironment; }
        public String getOrigin() { return origin; }
        public void setOrigin(String origin) { this.origin = origin; }
        public java.util.List<String> getAllowedTools() { return allowedTools; }
        public void setAllowedTools(java.util.List<String> allowedTools) { this.allowedTools = allowedTools == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(allowedTools); }
        public java.util.List<String> getDeniedTools() { return deniedTools; }
        public void setDeniedTools(java.util.List<String> deniedTools) { this.deniedTools = deniedTools == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(deniedTools); }
        public String getDefaultRisk() { return defaultRisk; }
        public void setDefaultRisk(String defaultRisk) { this.defaultRisk = defaultRisk; }
        public String getStalePolicy() { return stalePolicy; }
        public void setStalePolicy(String stalePolicy) { this.stalePolicy = stalePolicy; }
        public long getRequestTimeoutMs() { return requestTimeoutMs; }
        public void setRequestTimeoutMs(long requestTimeoutMs) { this.requestTimeoutMs = Math.max(1_000, requestTimeoutMs); }
        public Map<String, String> getSemanticMappings() { return semanticMappings; }
        public void setSemanticMappings(Map<String, String> semanticMappings) { this.semanticMappings = semanticMappings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(semanticMappings); }
        public Map<String, String> getCapabilityMappings() { return capabilityMappings; }
        public void setCapabilityMappings(Map<String, String> capabilityMappings) { this.capabilityMappings = capabilityMappings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(capabilityMappings); }
        public Map<String, String> getRiskMappings() { return riskMappings; }
        public void setRiskMappings(Map<String, String> riskMappings) { this.riskMappings = riskMappings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(riskMappings); }
    }

    public static class Sandbox {
        private boolean enabled = false;
        private String backend = "local-restricted";
        private String dockerImage = "ubuntu:24.04";
        private boolean networkEnabled = false;
        private long timeoutMs = 30_000;
        private int maxOutputChars = 20_000;
        private String workdirSubdir = "sandbox";
        private String allowedCommands = "mvn,npm,node,python,python3,java,javac,ls,pwd,cat,rg,grep";
        private String deniedPatterns = "rm -rf,format,format-volume,shutdown,reboot,del /s,Remove-Item -Recurse";
        private String allowedScriptLanguages = "python,powershell,bash";
        private String scriptWorkdirSubdir = "scripts";
        private boolean runScriptLocalUnsafeAllowed = false;
        private boolean allowHostExecution = false;
        private Map<String, String> executables = new LinkedHashMap<>();
        private LocalTrusted localTrusted = new LocalTrusted();
        private Map<String, String> environment = new LinkedHashMap<>();

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

        public boolean isAllowHostExecution() {
            return allowHostExecution;
        }

        public void setAllowHostExecution(boolean allowHostExecution) {
            this.allowHostExecution = allowHostExecution;
        }

        public Map<String, String> getExecutables() {
            return executables;
        }

        public void setExecutables(Map<String, String> executables) {
            this.executables = executables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(executables);
        }

        public LocalTrusted getLocalTrusted() {
            return localTrusted;
        }

        public void setLocalTrusted(LocalTrusted localTrusted) {
            this.localTrusted = localTrusted == null ? new LocalTrusted() : localTrusted;
        }

        public Map<String, String> getEnvironment() {
            return environment;
        }

        public void setEnvironment(Map<String, String> environment) {
            this.environment = environment == null ? new LinkedHashMap<>() : new LinkedHashMap<>(environment);
        }

        public static class LocalTrusted {
            private boolean enabled = false;
            private boolean inheritEnvironment = true;
            private boolean inheritPath = true;
            private boolean loadUserProfile = false;
            private boolean isolateHome = false;
            private String shellExecutable = "";
            private java.util.List<String> shellInitFiles = new java.util.ArrayList<>();
            private java.util.List<String> passthroughEnvironmentNames = new java.util.ArrayList<>();
            private java.util.List<String> deniedEnvironmentNames = new java.util.ArrayList<>();
            private java.util.List<String> deniedEnvironmentPatterns = new java.util.ArrayList<>();

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public boolean isInheritEnvironment() {
                return inheritEnvironment;
            }

            public void setInheritEnvironment(boolean inheritEnvironment) {
                this.inheritEnvironment = inheritEnvironment;
            }

            public boolean isInheritPath() {
                return inheritPath;
            }

            public void setInheritPath(boolean inheritPath) {
                this.inheritPath = inheritPath;
            }

            public boolean isLoadUserProfile() {
                return loadUserProfile;
            }

            public void setLoadUserProfile(boolean loadUserProfile) {
                this.loadUserProfile = loadUserProfile;
            }

            public boolean isIsolateHome() {
                return isolateHome;
            }

            public void setIsolateHome(boolean isolateHome) {
                this.isolateHome = isolateHome;
            }

            public String getShellExecutable() {
                return shellExecutable;
            }

            public void setShellExecutable(String shellExecutable) {
                this.shellExecutable = shellExecutable == null ? "" : shellExecutable;
            }

            public java.util.List<String> getShellInitFiles() {
                return shellInitFiles;
            }

            public void setShellInitFiles(java.util.List<String> shellInitFiles) {
                this.shellInitFiles = shellInitFiles == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(shellInitFiles);
            }

            public java.util.List<String> getPassthroughEnvironmentNames() {
                return passthroughEnvironmentNames;
            }

            public void setPassthroughEnvironmentNames(java.util.List<String> passthroughEnvironmentNames) {
                this.passthroughEnvironmentNames = passthroughEnvironmentNames == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(passthroughEnvironmentNames);
            }

            public java.util.List<String> getDeniedEnvironmentNames() {
                return deniedEnvironmentNames;
            }

            public void setDeniedEnvironmentNames(java.util.List<String> deniedEnvironmentNames) {
                this.deniedEnvironmentNames = deniedEnvironmentNames == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(deniedEnvironmentNames);
            }

            public java.util.List<String> getDeniedEnvironmentPatterns() {
                return deniedEnvironmentPatterns;
            }

            public void setDeniedEnvironmentPatterns(java.util.List<String> deniedEnvironmentPatterns) {
                this.deniedEnvironmentPatterns = deniedEnvironmentPatterns == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(deniedEnvironmentPatterns);
            }
        }
    }

    public static class Graph {
        private boolean enabled = true;
        private GraphRuntimeMode mode = GraphRuntimeMode.GRAPH_FIRST;
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
            this.mode = mode == null ? GraphRuntimeMode.GRAPH_FIRST : mode;
        }

        public Checkpoint getCheckpoint() {
            return checkpoint;
        }

        public void setCheckpoint(Checkpoint checkpoint) {
            this.checkpoint = checkpoint == null ? new Checkpoint() : checkpoint;
        }
    }

    public static class Checkpoint {
        private boolean enabled = true;

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

    public String getNetworkProxyUrl() {
        return networkProxyUrl;
    }

    public void setNetworkProxyUrl(String networkProxyUrl) {
        this.networkProxyUrl = networkProxyUrl;
    }

    public String getNetworkProxyUsername() {
        return networkProxyUsername;
    }

    public void setNetworkProxyUsername(String networkProxyUsername) {
        this.networkProxyUsername = networkProxyUsername;
    }

    public String getNetworkProxyPassword() {
        return networkProxyPassword;
    }

    public void setNetworkProxyPassword(String networkProxyPassword) {
        this.networkProxyPassword = networkProxyPassword;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getWorkspaceRoot() {
        return resolvePath(workspaceRoot, Path.of(getUserDataRoot()).resolve("workspace").toString()).toString();
    }

    public void setWorkspaceRoot(String workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    public String getUserDataRoot() {
        return resolvePath(userDataRoot, "${user.dir}/data/user-data").toString();
    }

    public void setUserDataRoot(String userDataRoot) {
        this.userDataRoot = userDataRoot;
    }

    public String getSkillsRoot() {
        return skillsRoot;
    }

    public void setSkillsRoot(String skillsRoot) {
        this.skillsRoot = skillsRoot;
    }

    public String getSkillsContainerPath() {
        return StringUtils.hasText(skillsContainerPath) ? skillsContainerPath : "/mnt/skills";
    }

    public void setSkillsContainerPath(String skillsContainerPath) {
        this.skillsContainerPath = skillsContainerPath;
    }

    public boolean isSkillsEnabled() {
        return skillsEnabled;
    }

    public void setSkillsEnabled(boolean skillsEnabled) {
        this.skillsEnabled = skillsEnabled;
    }

    public boolean isMcpEnabled() {
        return mcpEnabled || (mcp != null && mcp.isEnabled());
    }

    public void setMcpEnabled(boolean mcpEnabled) {
        this.mcpEnabled = mcpEnabled;
    }

    public Mcp getMcp() {
        return mcp;
    }

    public void setMcp(Mcp mcp) {
        this.mcp = mcp == null ? new Mcp() : mcp;
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
        return resolvePath(uploadsRoot, Path.of(getUserDataRoot()).resolve("uploads").toString()).toString();
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

    public RunConcurrencyMode getRunConcurrencyMode() {
        return runConcurrencyMode;
    }

    public void setRunConcurrencyMode(RunConcurrencyMode runConcurrencyMode) {
        this.runConcurrencyMode = runConcurrencyMode == null
                ? RunConcurrencyMode.ALLOW_PARALLEL_RUNS : runConcurrencyMode;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public int getMaxToolCalls() {
        return maxToolCalls;
    }

    public void setMaxToolCalls(int maxToolCalls) {
        this.maxToolCalls = maxToolCalls;
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
        return resolvePath(outputsRoot, Path.of(getUserDataRoot()).resolve("outputs").toString()).toString();
    }

    public void setOutputsRoot(String outputsRoot) {
        this.outputsRoot = outputsRoot;
    }

    private static Path resolvePath(String configured, String fallback) {
        String value = StringUtils.hasText(configured) ? configured : fallback;
        value = value.replace("${user.dir}", System.getProperty("user.dir"));
        return Path.of(value).toAbsolutePath().normalize();
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

    private PromptCache promptCache = new PromptCache();

    public PromptCache getPromptCache() {
        return promptCache;
    }

    public void setPromptCache(PromptCache promptCache) {
        this.promptCache = promptCache == null ? new PromptCache() : promptCache;
    }

    public static class PromptCache {
        private boolean enabled = true;
        private String canonicalizationVersion = "v1";
        private int routingShards = 1;
        private boolean observabilityEnabled = true;
        private OpenAiPromptCache openai = new OpenAiPromptCache();
        private GooglePromptCache google = new GooglePromptCache();
        private CompressionPromptCache compression = new CompressionPromptCache();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getCanonicalizationVersion() { return canonicalizationVersion; }
        public void setCanonicalizationVersion(String canonicalizationVersion) { this.canonicalizationVersion = canonicalizationVersion; }
        public int getRoutingShards() { return routingShards; }
        public void setRoutingShards(int routingShards) { this.routingShards = routingShards; }
        public boolean isObservabilityEnabled() { return observabilityEnabled; }
        public void setObservabilityEnabled(boolean observabilityEnabled) { this.observabilityEnabled = observabilityEnabled; }
        public OpenAiPromptCache getOpenai() { return openai; }
        public void setOpenai(OpenAiPromptCache openai) { this.openai = openai == null ? new OpenAiPromptCache() : openai; }
        public GooglePromptCache getGoogle() { return google; }
        public void setGoogle(GooglePromptCache google) { this.google = google == null ? new GooglePromptCache() : google; }
        public CompressionPromptCache getCompression() { return compression; }
        public void setCompression(CompressionPromptCache compression) { this.compression = compression == null ? new CompressionPromptCache() : compression; }

        public static class OpenAiPromptCache {
            private boolean enabled = true;
            private boolean promptCacheKeyEnabled = true;
            private String retention = "PROVIDER_DEFAULT";
            private boolean explicitBreakpointsEnabled = false;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public boolean isPromptCacheKeyEnabled() { return promptCacheKeyEnabled; }
            public void setPromptCacheKeyEnabled(boolean promptCacheKeyEnabled) { this.promptCacheKeyEnabled = promptCacheKeyEnabled; }
            public String getRetention() { return retention; }
            public void setRetention(String retention) { this.retention = retention; }
            public boolean isExplicitBreakpointsEnabled() { return explicitBreakpointsEnabled; }
            public void setExplicitBreakpointsEnabled(boolean explicitBreakpointsEnabled) { this.explicitBreakpointsEnabled = explicitBreakpointsEnabled; }
        }

        public static class GooglePromptCache {
            private boolean enabled = true;
            private boolean includeExtendedUsageMetadata = true;
            private boolean autoCacheEnabled = false;
            private int autoCacheThreshold = 8192;
            private String autoCacheTtl = "PT30M";
            private ExplicitContent explicitContent = new ExplicitContent();

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public boolean isIncludeExtendedUsageMetadata() { return includeExtendedUsageMetadata; }
            public void setIncludeExtendedUsageMetadata(boolean includeExtendedUsageMetadata) { this.includeExtendedUsageMetadata = includeExtendedUsageMetadata; }
            public boolean isAutoCacheEnabled() { return autoCacheEnabled; }
            public void setAutoCacheEnabled(boolean autoCacheEnabled) { this.autoCacheEnabled = autoCacheEnabled; }
            public int getAutoCacheThreshold() { return autoCacheThreshold; }
            public void setAutoCacheThreshold(int autoCacheThreshold) { this.autoCacheThreshold = autoCacheThreshold; }
            public String getAutoCacheTtl() { return autoCacheTtl; }
            public void setAutoCacheTtl(String autoCacheTtl) { this.autoCacheTtl = autoCacheTtl; }
            public ExplicitContent getExplicitContent() { return explicitContent; }
            public void setExplicitContent(ExplicitContent explicitContent) { this.explicitContent = explicitContent == null ? new ExplicitContent() : explicitContent; }

            public static class ExplicitContent {
                private boolean enabled = false;
                private int minEstimatedTokens = 8192;
                private String ttl = "PT30M";
                private String createWaitTimeout = "PT1S";

                public boolean isEnabled() { return enabled; }
                public void setEnabled(boolean enabled) { this.enabled = enabled; }
                public int getMinEstimatedTokens() { return minEstimatedTokens; }
                public void setMinEstimatedTokens(int minEstimatedTokens) { this.minEstimatedTokens = minEstimatedTokens; }
                public String getTtl() { return ttl; }
                public void setTtl(String ttl) { this.ttl = ttl; }
                public String getCreateWaitTimeout() { return createWaitTimeout; }
                public void setCreateWaitTimeout(String createWaitTimeout) { this.createWaitTimeout = createWaitTimeout; }
            }
        }

        public static class CompressionPromptCache {
            private boolean enabled = true;
            private String schemaVersion = "v1";
            private String promptVersion = "evidence-compressor-v1";
            private String ttl = "P7D";
            private int maxEntries = 10000;
            private String cleanupInterval = "PT6H";

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getSchemaVersion() { return schemaVersion; }
            public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
            public String getPromptVersion() { return promptVersion; }
            public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
            public String getTtl() { return ttl; }
            public void setTtl(String ttl) { this.ttl = ttl; }
            public int getMaxEntries() { return maxEntries; }
            public void setMaxEntries(int maxEntries) { this.maxEntries = maxEntries; }
            public String getCleanupInterval() { return cleanupInterval; }
            public void setCleanupInterval(String cleanupInterval) { this.cleanupInterval = cleanupInterval; }
        }
    }
}
