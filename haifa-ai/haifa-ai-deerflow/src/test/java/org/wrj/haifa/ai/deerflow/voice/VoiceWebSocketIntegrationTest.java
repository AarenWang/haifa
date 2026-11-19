package org.wrj.haifa.ai.deerflow.voice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.wrj.haifa.ai.deerflow.voice.application.VoiceProtocol;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "haifa.ai.deerflow.tools.web-search.api-key=fake-key",
                "haifa.ai.deerflow.tools.web-fetch.api-key=fake-key"
        }
)
public class VoiceWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    public void testWebSocketVoiceTurnFlow() throws Exception {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        URI uri = new URI("ws://localhost:" + port + "/api/deerflow/voice/ws");

        List<String> receivedControls = new ArrayList<>();

        String msg1 = VoiceProtocol.encodeControlJson(
                new VoiceProtocol.ControlMessage("session.create", null, null, "test_thread", null, null, null, null, null, null));
        String msg2 = VoiceProtocol.encodeControlJson(
                new VoiceProtocol.ControlMessage("turn.start", null, "turn_test_1", "test_thread", null, null, null, null, null, null));
        String msg3 = VoiceProtocol.encodeControlJson(
                new VoiceProtocol.ControlMessage("turn.commit", null, "turn_test_1", "test_thread", null, null, null, null, null, null));

        Mono<Void> sessionMono = client.execute(uri, wsSession -> {
            Mono<Void> receive = wsSession.receive()
                    .filter(msg -> msg.getType() == WebSocketMessage.Type.TEXT)
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(receivedControls::add)
                    .take(2) // expects session.ready and asr.ready
                    .then();

            Flux<WebSocketMessage> outbound = Flux.just(
                    wsSession.textMessage(msg1),
                    wsSession.textMessage(msg2),
                    wsSession.textMessage(msg3)
            );

            return wsSession.send(outbound).then(receive);
        });

        sessionMono.block(Duration.ofSeconds(10));

        assertFalse(receivedControls.isEmpty());
        assertTrue(receivedControls.stream().anyMatch(c -> c.contains("session.ready")));
        assertTrue(receivedControls.stream().anyMatch(c -> c.contains("asr.ready")));
    }
}
