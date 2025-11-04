package org.wrj.haifa.openfeign.broadcast.server;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wrj.haifa.openfeign.broadcast.shared.BroadcastMessageRequest;
import org.wrj.haifa.openfeign.broadcast.shared.MessageAck;
import org.wrj.haifa.openfeign.broadcast.shared.MessageRecord;

@RestController
@RequestMapping("/api/messages")
class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    private final InMemoryMessageRepository repository;
    private final String instanceId;

    MessageController(
            InMemoryMessageRepository repository,
            @Value("${spring.cloud.nacos.discovery.instance-id:}") String configuredInstanceId,
            @Value("${spring.application.name}") String applicationName,
            @Value("${server.port}") int serverPort) {
        this.repository = repository;
        this.instanceId = (configuredInstanceId != null && !configuredInstanceId.isBlank())
                ? configuredInstanceId
                : applicationName + ":" + serverPort;
    }

    @PostMapping
    MessageAck publish(@RequestBody BroadcastMessageRequest request) {
        Instant now = Instant.now();
        int total = repository.append(request.message(), request.origin(), now);
        log.info(String.format(
                "Stored broadcast message '%s' from %s (total messages: %d)",
                request.message(),
                request.origin(),
                Integer.valueOf(total)));
        return new MessageAck(instanceId, total, now);
    }

    @GetMapping
    List<MessageRecord> list() {
        return repository.list();
    }
}
