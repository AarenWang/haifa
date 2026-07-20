package org.wrj.haifa.ai.utilitymcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

class CalculatorServiceTest {

    private final CalculatorService service = new CalculatorService();

    @Test
    void evaluatesPrecedenceFunctionsAndDecimals() {
        assertThat(service.calculate(Map.of("expression", "2 + 3 * pow(4, 2) / 8")).data())
                .containsEntry("result", "8");
        assertThat(service.calculate(Map.of("expression", "0.1 + 0.2")).data())
                .containsEntry("result", "0.3");
    }

    @Test
    void rejectsCodeAndDivisionByZero() {
        assertThatThrownBy(() -> service.calculate(Map.of("expression", "java.lang.Runtime.getRuntime()")))
                .isInstanceOf(UtilityToolException.class);
        assertThatThrownBy(() -> service.calculate(Map.of("expression", "1 / 0")))
                .isInstanceOf(UtilityToolException.class)
                .hasMessageContaining("division by zero");
    }

    @Test
    void rejectsExponentBomb() {
        assertThatThrownBy(() -> service.calculate(Map.of("expression", "2 ^ 1001")))
                .isInstanceOf(UtilityToolException.class)
                .hasMessageContaining("exponent");
    }
}
