package org.wrj.haifa.ai.deerflow.tool.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.persistence.entity.ToolActionExecutionEntity;
import org.wrj.haifa.ai.deerflow.persistence.repository.ToolActionExecutionRepository;

class ToolExecutionIdempotencyServiceTest {
    private final Map<String, ToolActionExecutionEntity> database = new HashMap<>();
    private ToolExecutionIdempotencyService service;

    @BeforeEach
    void setUp() {
        ToolActionExecutionRepository repository = mock(ToolActionExecutionRepository.class);
        when(repository.findById(any())).thenAnswer(call -> Optional.ofNullable(database.get(call.getArgument(0))));
        when(repository.save(any())).thenAnswer(call -> store(call.getArgument(0)));
        when(repository.saveAndFlush(any())).thenAnswer(call -> store(call.getArgument(0)));
        service = new ToolExecutionIdempotencyService(repository);
    }

    @Test
    void replaysSuccessfulResultWithoutExecutingAgain() {
        var first = service.reserve("run-1", "call-1", " Web_Search ", " { \"q\": 1 } ", false);
        assertThat(first.decision()).isEqualTo(ToolExecutionIdempotencyService.Decision.EXECUTE);
        service.markRunning(first.idempotencyKey());
        service.markSucceeded(first.idempotencyKey(), "persisted-result");

        var replay = service.reserve("run-1", "call-1", "web_search", "{ \"q\": 1 }", false);

        assertThat(replay.decision()).isEqualTo(ToolExecutionIdempotencyService.Decision.REPLAY_SUCCEEDED);
        assertThat(replay.result()).isEqualTo("persisted-result");
    }

    @Test
    void blocksHighRiskRetryAfterCrashWindow() {
        var first = service.reserve("run-2", "call-2", "write_file", "{}", true);
        service.markRunning(first.idempotencyKey());

        var resumed = service.reserve("run-2", "call-2", "write_file", "{}", true);

        assertThat(resumed.decision())
                .isEqualTo(ToolExecutionIdempotencyService.Decision.BLOCK_UNKNOWN_OUTCOME);
        assertThat(database.get(first.idempotencyKey()).getStatus())
                .isEqualTo(ToolExecutionStatus.UNKNOWN_OUTCOME);
    }

    private ToolActionExecutionEntity store(ToolActionExecutionEntity entity) {
        if (entity.getCreatedAt() == null) entity.setCreatedAt(Instant.now());
        database.put(entity.getIdempotencyKey(), entity);
        return entity;
    }
}
