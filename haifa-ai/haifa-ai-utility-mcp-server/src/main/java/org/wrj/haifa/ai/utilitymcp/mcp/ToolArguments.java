package org.wrj.haifa.ai.utilitymcp.mcp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

public final class ToolArguments {

    private final Map<String, Object> values;

    public ToolArguments(Map<String, Object> values) {
        this.values = values == null ? Map.of() : values;
    }

    public String requiredString(String name, int maxLength) {
        String value = optionalString(name, null, maxLength);
        if (value == null || value.isBlank()) {
            throw UtilityToolException.invalid(name + " is required");
        }
        return value;
    }

    public String optionalString(String name, String defaultValue, int maxLength) {
        Object raw = values.get(name);
        if (raw == null) {
            return defaultValue;
        }
        if (!(raw instanceof String value) || value.isBlank() || value.length() > maxLength) {
            throw UtilityToolException.invalid(name + " must be a non-empty string of at most " + maxLength + " characters");
        }
        return value;
    }

    public String enumValue(String name, String defaultValue, java.util.Set<String> allowed) {
        String value = optionalString(name, defaultValue, 64).toUpperCase(Locale.ROOT);
        if (!allowed.contains(value)) {
            throw new UtilityToolException(UtilityErrorCode.UNSUPPORTED_VALUE,
                    name + " must be one of " + allowed, false);
        }
        return value;
    }

    public int intValue(String name, int defaultValue, int min, int max) {
        Object raw = values.get(name);
        int value;
        if (raw == null) {
            value = defaultValue;
        }
        else if (raw instanceof Number number) {
            value = number.intValue();
        }
        else {
            throw UtilityToolException.invalid(name + " must be an integer");
        }
        if (value < min || value > max) {
            throw UtilityToolException.invalid(name + " must be between " + min + " and " + max);
        }
        return value;
    }

    public double doubleValue(String name, double min, double max) {
        Object raw = values.get(name);
        if (!(raw instanceof Number number)) {
            throw UtilityToolException.invalid(name + " must be a number");
        }
        double value = number.doubleValue();
        if (!Double.isFinite(value) || value < min || value > max) {
            throw UtilityToolException.invalid(name + " must be between " + min + " and " + max);
        }
        return value;
    }

    public BigDecimal decimal(String name, boolean allowNegative) {
        Object raw = values.get(name);
        if (raw == null) {
            throw UtilityToolException.invalid(name + " is required");
        }
        try {
            BigDecimal value = raw instanceof BigDecimal decimal ? decimal : new BigDecimal(raw.toString());
            if (value.precision() > 100 || value.scale() > 50 || value.abs().compareTo(new BigDecimal("1E100")) > 0) {
                throw UtilityToolException.invalid(name + " exceeds the supported precision or magnitude");
            }
            if (!allowNegative && value.signum() < 0) {
                throw UtilityToolException.invalid(name + " must not be negative");
            }
            return value;
        }
        catch (NumberFormatException ex) {
            throw UtilityToolException.invalid(name + " must be a decimal number");
        }
    }

    public LocalDate date(String name, LocalDate defaultValue) {
        String raw = optionalString(name, null, 10);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(raw);
        }
        catch (RuntimeException ex) {
            throw UtilityToolException.invalid(name + " must use ISO-8601 yyyy-MM-dd format");
        }
    }
}
