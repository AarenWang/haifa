package org.wrj.haifa.ai.utilitymcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

class TimeNowToolTest {

    private final TimeNowTool tool = new TimeNowTool(
            Clock.fixed(Instant.parse("2026-07-20T02:00:00Z"), ZoneOffset.UTC));

    @Test
    void returnsStructuredTimeInRequestedZone() {
        var result = tool.execute(Map.of("timezone", "Asia/Shanghai"));

        assertThat(result.data())
                .containsEntry("instant", "2026-07-20T02:00:00Z")
                .containsEntry("localDateTime", "2026-07-20T10:00")
                .containsEntry("timezone", "Asia/Shanghai")
                .containsEntry("utcOffset", "+08:00");
    }

    @Test
    void defaultsToUtc() {
        assertThat(tool.execute(Map.of()).data()).containsEntry("timezone", "UTC");
    }

    @Test
    void rejectsInvalidTimezone() {
        assertThatThrownBy(() -> tool.execute(Map.of("timezone", "Mars/Olympus")))
                .isInstanceOf(UtilityToolException.class)
                .hasMessageContaining("valid IANA timezone");
    }

    @Test
    void publishesObjectSchemaAndSafeAnnotations() {
        assertThat(tool.contract().name()).isEqualTo("time_now");
        assertThat(tool.contract().inputSchema().type()).isEqualTo("object");
        assertThat(tool.contract().inputSchema().additionalProperties()).isFalse();
        assertThat(tool.contract().annotations().readOnlyHint()).isTrue();
        assertThat(tool.contract().annotations().openWorldHint()).isFalse();
    }
}
