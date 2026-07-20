package org.wrj.haifa.ai.utilitymcp.tool;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.wrj.haifa.ai.utilitymcp.mcp.ToolArguments;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityErrorCode;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityResult;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

public class UnitConversionService {

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);
    private final Map<String, Unit> units;

    public UnitConversionService() {
        Map<String, Unit> registry = new LinkedHashMap<>();
        add(registry, "length", "m", "1", "meter", "metre", "meters", "metres");
        add(registry, "length", "km", "1000", "kilometer", "kilometre");
        add(registry, "length", "cm", "0.01", "centimeter", "centimetre");
        add(registry, "length", "mm", "0.001", "millimeter", "millimetre");
        add(registry, "length", "in", "0.0254", "inch", "inches");
        add(registry, "length", "ft", "0.3048", "foot", "feet");
        add(registry, "length", "mi", "1609.344", "mile", "miles");
        add(registry, "mass", "kg", "1", "kilogram");
        add(registry, "mass", "g", "0.001", "gram");
        add(registry, "mass", "mg", "0.000001", "milligram");
        add(registry, "mass", "lb", "0.45359237", "pound", "pounds");
        add(registry, "mass", "oz", "0.028349523125", "ounce", "ounces");
        add(registry, "time", "s", "1", "second", "seconds");
        add(registry, "time", "min", "60", "minute", "minutes");
        add(registry, "time", "h", "3600", "hour", "hours");
        add(registry, "time", "day", "86400", "days");
        add(registry, "speed", "m/s", "1", "mps");
        add(registry, "speed", "km/h", "0.2777777777777777777777777777777778", "kph");
        add(registry, "speed", "mph", "0.44704");
        add(registry, "data_size", "B", "1", "byte", "bytes");
        add(registry, "data_size", "KB", "1000");
        add(registry, "data_size", "MB", "1000000");
        add(registry, "data_size", "GB", "1000000000");
        add(registry, "data_size", "KiB", "1024");
        add(registry, "data_size", "MiB", "1048576");
        add(registry, "data_size", "GiB", "1073741824");
        temperature(registry, "C", BigDecimal.ONE, new BigDecimal("273.15"), "celsius", "°c");
        temperature(registry, "K", BigDecimal.ONE, BigDecimal.ZERO, "kelvin");
        temperature(registry, "F", new BigDecimal("0.5555555555555555555555555555555556"),
                new BigDecimal("255.3722222222222222222222222222222"), "fahrenheit", "°f");
        this.units = Map.copyOf(registry);
    }

    public UtilityResult convert(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        BigDecimal value = args.decimal("value", true);
        String fromName = args.requiredString("fromUnit", 32);
        String toName = args.requiredString("toUnit", 32);
        int scale = args.intValue("scale", 12, 0, 30);
        RoundingMode rounding;
        try {
            rounding = RoundingMode.valueOf(args.optionalString("roundingMode", "HALF_EVEN", 32).toUpperCase(Locale.ROOT));
        }
        catch (RuntimeException ex) {
            throw UtilityToolException.invalid("roundingMode is not supported");
        }
        Unit from = lookup(fromName);
        Unit to = lookup(toName);
        if (!from.dimension.equals(to.dimension)) {
            throw new UtilityToolException(UtilityErrorCode.UNSUPPORTED_VALUE,
                    "fromUnit and toUnit must belong to the same dimension", false);
        }
        BigDecimal base = value.multiply(from.factor, MC).add(from.offset, MC);
        if (from.dimension.equals("temperature") && base.signum() < 0) {
            throw UtilityToolException.invalid("temperature is below absolute zero");
        }
        BigDecimal converted = base.subtract(to.offset, MC).divide(to.factor, MC).setScale(scale, rounding);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("inputValue", value.toPlainString());
        data.put("fromUnit", from.canonical);
        data.put("outputValue", converted.stripTrailingZeros().toPlainString());
        data.put("toUnit", to.canonical);
        data.put("dimension", from.dimension);
        data.put("scale", scale);
        data.put("roundingMode", rounding.name());
        data.put("formula", from.dimension.equals("temperature") ? "affine via kelvin" : "ratio via canonical base unit");
        return UtilityResult.local(data);
    }

    private Unit lookup(String name) {
        Unit unit = units.get(normalize(name));
        if (unit == null) {
            throw new UtilityToolException(UtilityErrorCode.UNSUPPORTED_VALUE, "unsupported unit: " + name, false);
        }
        return unit;
    }

    private static void add(Map<String, Unit> registry, String dimension, String canonical, String factor, String... aliases) {
        register(registry, new Unit(dimension, canonical, new BigDecimal(factor), BigDecimal.ZERO), aliases);
    }

    private static void temperature(Map<String, Unit> registry, String canonical, BigDecimal factor,
            BigDecimal offset, String... aliases) {
        register(registry, new Unit("temperature", canonical, factor, offset), aliases);
    }

    private static void register(Map<String, Unit> registry, Unit unit, String... aliases) {
        put(registry, unit.canonical, unit);
        for (String alias : aliases) put(registry, alias, unit);
    }

    private static void put(Map<String, Unit> registry, String name, Unit unit) {
        if (registry.putIfAbsent(normalize(name), unit) != null) {
            throw new IllegalStateException("Duplicate unit alias: " + name);
        }
    }

    private static String normalize(String value) {
        String trimmed = value.trim();
        if (trimmed.equals("B") || trimmed.endsWith("B") || trimmed.endsWith("iB")) return trimmed;
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private record Unit(String dimension, String canonical, BigDecimal factor, BigDecimal offset) {}
}
