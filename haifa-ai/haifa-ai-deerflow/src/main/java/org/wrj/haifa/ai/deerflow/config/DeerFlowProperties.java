package org.wrj.haifa.ai.deerflow.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "haifa.ai.deerflow")
public class DeerFlowProperties {

    private String model;
    private String systemPrompt;
    private String workspaceRoot = ".";
    private String skillsRoot = ".";
    private boolean skillsEnabled = true;
    private boolean mcpEnabled = false;
    private boolean toolSearchEnabled = true;

    private String uploadsRoot = "${user.dir}/uploads";
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
        return workspaceRoot;
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
}
