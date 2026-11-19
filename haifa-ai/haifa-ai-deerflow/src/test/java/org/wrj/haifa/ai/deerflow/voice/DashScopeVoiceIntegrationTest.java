package org.wrj.haifa.ai.deerflow.voice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.wrj.haifa.ai.deerflow.voice.application.VoiceProtocol;
import org.wrj.haifa.ai.deerflow.voice.domain.AsrEvent;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioChunk;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioFormat;
import org.wrj.haifa.ai.deerflow.voice.domain.TextChunk;
import org.wrj.haifa.ai.deerflow.voice.provider.AsrSession;
import org.wrj.haifa.ai.deerflow.voice.provider.AsrStartOptions;
import org.wrj.haifa.ai.deerflow.voice.provider.TtsSession;
import org.wrj.haifa.ai.deerflow.voice.provider.TtsStartOptions;
import org.wrj.haifa.ai.deerflow.voice.provider.dashscope.DashScopeStreamingAsrProvider;
import org.wrj.haifa.ai.deerflow.voice.provider.dashscope.DashScopeStreamingTtsProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 阿里云百炼 (DashScope) 语音模型链路单独集成测试。
 * 
 * 环境变量约定依据：docs/voice-conversation.md
 * - DASHSCOPE_API_KEY
 * - DASHSCOPE_WORKSPACE_ID
 * - DASHSCOPE_REGION
 * - DASHSCOPE_ASR_MODEL
 * - DASHSCOPE_TTS_MODEL
 * - DASHSCOPE_TTS_VOICE
 * - DASHSCOPE_TTS_SAMPLE_RATE
 * 
 * 未配置 DASHSCOPE_API_KEY 时自动跳过测试（Pass/Skipped），配置后直接打通百炼云端真实 ASR & TTS 接口。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "haifa.ai.deerflow.tools.web-search.api-key=fake-key",
                "haifa.ai.deerflow.tools.web-fetch.api-key=fake-key",
                "haifa.ai.deerflow.voice.asr.providers.dashscope.enabled=true",
                "haifa.ai.deerflow.voice.tts.providers.dashscope.enabled=true"
        }
)
public class DashScopeVoiceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(DashScopeVoiceIntegrationTest.class);

    @LocalServerPort
    private int port;

    @Autowired
    private VoiceProperties voiceProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DashScopeStreamingAsrProvider asrProvider;

    @Autowired
    private DashScopeStreamingTtsProvider ttsProvider;

    @BeforeEach
    public void checkEnvironmentVariables() {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = voiceProperties.getAsr().getProviders().getDashscope().getApiKey();
        }
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "跳过阿里云百炼语音测试：未配置 DASHSCOPE_API_KEY 环境变量。可配置 $env:DASHSCOPE_API_KEY='sk-xxx' 后运行本测试打通百炼。");
    }

    @Test
    @DisplayName("测试直接连接百炼流式 ASR (Paraformer / Qwen ASR)")
    public void testDashScopeAsrDirectly() throws Exception {
        log.info("开始连接百炼 ASR 接口，模型: {}", asrProvider.model());
        assertTrue(asrProvider.isAvailable(), "DashScope ASR Provider 应处于可用状态");

        AsrStartOptions options = new AsrStartOptions("zh", AudioFormat.DEFAULT_INPUT, null, Map.of());
        AsrSession session = asrProvider.open(options).block(Duration.ofSeconds(15));
        assertNotNull(session, "ASR Session 打开不应为空");

        List<AsrEvent> events = new ArrayList<>();
        session.events().subscribe(events::add, err -> log.error("ASR 事件异常", err));

        // 发送 3 段示例 16kHz PCM 数据帧
        byte[] pcmChunk = new byte[3200];
        for (int i = 0; i < 3; i++) {
            session.append(pcmChunk).block(Duration.ofSeconds(5));
            Thread.sleep(50);
        }

        session.commit().block(Duration.ofSeconds(10));
        log.info("ASR 提交完成，收到 {} 个 ASR 事件", events.size());
        assertFalse(events.isEmpty(), "应当收到至少 1 个 ASR 事件");
    }

    @Test
    @DisplayName("测试直接连接百炼流式 TTS (CosyVoice TTS)")
    public void testDashScopeTtsDirectly() throws Exception {
        log.info("开始连接百炼 TTS 接口，模型: {}", ttsProvider.model());
        assertTrue(ttsProvider.isAvailable(), "DashScope TTS Provider 应处于可用状态");

        String voice = voiceProperties.getTts().getProviders().getDashscope().getVoice();
        TtsStartOptions options = new TtsStartOptions(
                voice, 1.0, null, new AudioFormat("pcm_s16le", 24000, 1, 16, 100), Map.of());

        TtsSession session = ttsProvider.open(options).block(Duration.ofSeconds(15));
        assertNotNull(session, "TTS Session 打开不应为空");

        List<AudioChunk> receivedAudio = new ArrayList<>();
        session.audio().subscribe(receivedAudio::add, err -> log.error("TTS 音频输出异常", err));

        // 发送测试文本
        TextChunk textChunk = new TextChunk(1, "你好，这里是海法 DeerFlow 阿里云百炼流式语音合成测试。", true);
        session.append(textChunk).block(Duration.ofSeconds(5));
        session.commit().block(Duration.ofSeconds(10));

        log.info("TTS 提交完成，共接收到 {} 段 24kHz PCM 音频帧", receivedAudio.size());
        assertFalse(receivedAudio.isEmpty(), "应当接收到百炼生成的 PCM 音频帧");
        assertTrue(receivedAudio.get(0).data().length > 0, "音频帧数据不应为空");
    }

    @Test
    @DisplayName("测试基于 WebSocket 完整的百炼双向语音 Turn 交互")
    public void testDashScopeEndToEndWebSocketTurn() throws Exception {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        URI uri = new URI("ws://localhost:" + port + "/api/deerflow/voice/ws");

        List<String> receivedControls = new ArrayList<>();
        List<byte[]> receivedAudioFrames = new ArrayList<>();

        String msgCreate = VoiceProtocol.encodeControlJson(
                new VoiceProtocol.ControlMessage("session.create", null, null, "dashscope_test_thread", null, null, null, null, null,
                        Map.of("asrProvider", "dashscope", "ttsProvider", "dashscope")));
        String msgStart = VoiceProtocol.encodeControlJson(
                new VoiceProtocol.ControlMessage("turn.start", null, "dashscope_turn_1", "dashscope_test_thread", null, null, null, null, null, null));
        String msgCommit = VoiceProtocol.encodeControlJson(
                new VoiceProtocol.ControlMessage("turn.commit", null, "dashscope_turn_1", "dashscope_test_thread", null, null, null, null, null, null));

        Mono<Void> sessionMono = client.execute(uri, wsSession -> {
            Mono<Void> receive = wsSession.receive()
                    .doOnNext(msg -> {
                        if (msg.getType() == WebSocketMessage.Type.TEXT) {
                            receivedControls.add(msg.getPayloadAsText());
                        } else if (msg.getType() == WebSocketMessage.Type.BINARY) {
                            byte[] bytes = new byte[msg.getPayload().readableByteCount()];
                            msg.getPayload().read(bytes);
                            receivedAudioFrames.add(bytes);
                        }
                    })
                    .takeUntil(msg -> msg.getType() == WebSocketMessage.Type.TEXT && msg.getPayloadAsText().contains("turn.completed"))
                    .then();

            Flux<WebSocketMessage> outbound = Flux.just(
                    wsSession.textMessage(msgCreate),
                    wsSession.textMessage(msgStart),
                    wsSession.textMessage(msgCommit)
            );

            return wsSession.send(outbound).then(receive);
        });

        sessionMono.block(Duration.ofSeconds(30));

        assertFalse(receivedControls.isEmpty(), "应该收到控制消息");
        assertTrue(receivedControls.stream().anyMatch(c -> c.contains("session.ready")), "应当收到 session.ready");
    }
}
