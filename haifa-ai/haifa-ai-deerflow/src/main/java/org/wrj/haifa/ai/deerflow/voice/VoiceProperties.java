package org.wrj.haifa.ai.deerflow.voice;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "haifa.ai.deerflow.voice")
public class VoiceProperties {

    private boolean enabled = false;
    private final Websocket websocket = new Websocket();
    private final Audio audio = new Audio();
    private final Asr asr = new Asr();
    private final Tts tts = new Tts();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Websocket getWebsocket() { return websocket; }
    public Audio getAudio() { return audio; }
    public Asr getAsr() { return asr; }
    public Tts getTts() { return tts; }

    public static class Websocket {
        private String path = "/api/deerflow/voice/ws";
        private int maxSessionsPerUser = 2;
        private int maxFrameBytes = 8192;
        private Duration idleTimeout = Duration.ofSeconds(90);

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public int getMaxSessionsPerUser() { return maxSessionsPerUser; }
        public void setMaxSessionsPerUser(int maxSessionsPerUser) { this.maxSessionsPerUser = maxSessionsPerUser; }
        public int getMaxFrameBytes() { return maxFrameBytes; }
        public void setMaxFrameBytes(int maxFrameBytes) { this.maxFrameBytes = maxFrameBytes; }
        public Duration getIdleTimeout() { return idleTimeout; }
        public void setIdleTimeout(Duration idleTimeout) { this.idleTimeout = idleTimeout; }
    }

    public static class Audio {
        private int inputSampleRateHz = 16000;
        private int inputChannels = 1;
        private Duration maxTurnDuration = Duration.ofSeconds(90);
        private long maxTurnBytes = 2_880_000;

        public int getInputSampleRateHz() { return inputSampleRateHz; }
        public void setInputSampleRateHz(int inputSampleRateHz) { this.inputSampleRateHz = inputSampleRateHz; }
        public int getInputChannels() { return inputChannels; }
        public void setInputChannels(int inputChannels) { this.inputChannels = inputChannels; }
        public Duration getMaxTurnDuration() { return maxTurnDuration; }
        public void setMaxTurnDuration(Duration maxTurnDuration) { this.maxTurnDuration = maxTurnDuration; }
        public long getMaxTurnBytes() { return maxTurnBytes; }
        public void setMaxTurnBytes(long maxTurnBytes) { this.maxTurnBytes = maxTurnBytes; }
    }

    public static class Asr {
        private String defaultProvider = "fake";
        private final AsrProviders providers = new AsrProviders();

        public String getDefaultProvider() { return defaultProvider; }
        public void setDefaultProvider(String defaultProvider) { this.defaultProvider = defaultProvider; }
        public AsrProviders getProviders() { return providers; }
    }

    public static class AsrProviders {
        private final DashScope dashscope = new DashScope();
        private final VolcengineAsr volcengine = new VolcengineAsr();

        public DashScope getDashscope() { return dashscope; }
        public VolcengineAsr getVolcengine() { return volcengine; }
    }

    public static class Tts {
        private String defaultProvider = "fake";
        private final TtsProviders providers = new TtsProviders();

        public String getDefaultProvider() { return defaultProvider; }
        public void setDefaultProvider(String defaultProvider) { this.defaultProvider = defaultProvider; }
        public TtsProviders getProviders() { return providers; }
    }

    public static class TtsProviders {
        private final DashScope dashscope = new DashScope();
        private final VolcengineTts volcengine = new VolcengineTts();

        public DashScope getDashscope() { return dashscope; }
        public VolcengineTts getVolcengine() { return volcengine; }
    }

    public static class DashScope {
        private boolean enabled;
        private String apiKey = "";
        private String workspaceId = "";
        private String region = "cn-beijing";
        private String endpoint = "";
        private String model = "";
        private String voice = "";
        private int sampleRateHz = 24000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getWorkspaceId() { return workspaceId; }
        public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getVoice() { return voice; }
        public void setVoice(String voice) { this.voice = voice; }
        public int getSampleRateHz() { return sampleRateHz; }
        public void setSampleRateHz(int sampleRateHz) { this.sampleRateHz = sampleRateHz; }

        public String realtimeEndpoint() {
            if (endpoint != null && !endpoint.isBlank()) {
                return endpoint;
            }
            String domainRegion = "ap-southeast-1".equalsIgnoreCase(region)
                    ? "ap-southeast-1"
                    : "cn-beijing";
            if (workspaceId != null && !workspaceId.isBlank()) {
                return "wss://" + workspaceId + "." + domainRegion + ".maas.aliyuncs.com/api-ws/v1/realtime";
            }
            return "ap-southeast-1".equalsIgnoreCase(region)
                    ? "wss://dashscope-ap-southeast-1.aliyuncs.com/api-ws/v1/realtime"
                    : "wss://dashscope.aliyuncs.com/api-ws/v1/realtime";
        }

        public String inferenceEndpoint() {
            String base = realtimeEndpoint();
            return base.endsWith("/realtime")
                    ? base.substring(0, base.length() - "/realtime".length()) + "/inference"
                    : base;
        }
    }

    public static class VolcengineAsr {
        private boolean enabled;
        private String apiKey = "";
        private String endpoint = "wss://ai-gateway.vei.volces.com/v1/realtime";
        private String model = "bigmodel";
        private String resourceId = "";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    }

    public static class VolcengineTts {
        private boolean enabled;
        private String appId = "";
        private String accessToken = "";
        private String endpoint = "wss://openspeech.bytedance.com/api/v1/tts/ws_binary";
        private String cluster = "volcano_tts";
        private String voice = "";
        private int sampleRateHz = 24000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getCluster() { return cluster; }
        public void setCluster(String cluster) { this.cluster = cluster; }
        public String getVoice() { return voice; }
        public void setVoice(String voice) { this.voice = voice; }
        public int getSampleRateHz() { return sampleRateHz; }
        public void setSampleRateHz(int sampleRateHz) { this.sampleRateHz = sampleRateHz; }
    }
}
