package org.wrj.haifa.ai.utilitymcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

class TimeServiceTest {

    private final TimeService service = new TimeService(
            Clock.fixed(Instant.parse("2026-07-20T02:00:00Z"), ZoneOffset.UTC));

    @Test
    void convertsAcrossTimezones() {
        var result = service.convert(Map.of(
                "dateTime", "2026-07-20T10:00:00",
                "fromTimezone", "Asia/Shanghai",
                "toTimezone", "America/New_York"));
        assertThat(result.data()).containsEntry("instant", "2026-07-20T02:00:00Z")
                .containsEntry("localDateTime", "2026-07-19T22:00");
    }

    @Test
    void rejectsDstGap() {
        assertThatThrownBy(() -> service.convert(Map.of(
                "dateTime", "2026-03-08T02:30:00",
                "fromTimezone", "America/New_York",
                "toTimezone", "UTC")))
                .isInstanceOf(UtilityToolException.class)
                .hasMessageContaining("daylight-saving gap");
    }

    @Test
    void requiresPolicyForDstOverlap() {
        assertThatThrownBy(() -> service.convert(Map.of(
                "dateTime", "2026-11-01T01:30:00",
                "fromTimezone", "America/New_York",
                "toTimezone", "UTC")))
                .isInstanceOf(UtilityToolException.class)
                .hasMessageContaining("ambiguous");
    }
}
