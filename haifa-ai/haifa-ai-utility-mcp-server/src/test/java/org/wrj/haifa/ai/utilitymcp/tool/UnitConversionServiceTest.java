package org.wrj.haifa.ai.utilitymcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

class UnitConversionServiceTest {

    private final UnitConversionService service = new UnitConversionService();

    @Test
    void convertsRatioTemperatureAndIecUnits() {
        assertThat(service.convert(Map.of("value", 1, "fromUnit", "km", "toUnit", "m")).data())
                .containsEntry("outputValue", "1000");
        assertThat(service.convert(Map.of("value", 32, "fromUnit", "F", "toUnit", "C")).data())
                .containsEntry("outputValue", "0");
        assertThat(service.convert(Map.of("value", 1, "fromUnit", "MiB", "toUnit", "B")).data())
                .containsEntry("outputValue", "1048576");
    }

    @Test
    void rejectsCrossDimensionAndBelowAbsoluteZero() {
        assertThatThrownBy(() -> service.convert(Map.of("value", 1, "fromUnit", "kg", "toUnit", "m")))
                .isInstanceOf(UtilityToolException.class)
                .hasMessageContaining("same dimension");
        assertThatThrownBy(() -> service.convert(Map.of("value", -1, "fromUnit", "K", "toUnit", "C")))
                .isInstanceOf(UtilityToolException.class)
                .hasMessageContaining("absolute zero");
    }
}
