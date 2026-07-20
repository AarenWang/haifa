package org.wrj.haifa.ai.utilitymcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.wrj.haifa.ai.utilitymcp.mcp.ToolArguments;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityErrorCode;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityResult;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;
import org.wrj.haifa.ai.utilitymcp.provider.JsonProvider;
import org.wrj.haifa.ai.utilitymcp.provider.ProviderPayload;

public class CurrencyService {

    private final JsonProvider provider;

    public CurrencyService(JsonProvider provider) {
        this.provider = provider;
    }

    public UtilityResult rate(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        String base = currency(args.requiredString("baseCurrency", 3));
        String quote = currency(args.requiredString("quoteCurrency", 3));
        LocalDate date = args.date("date", null);
        Rate rate = load(base, quote, date);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("baseCurrency", base);
        data.put("quoteCurrency", quote);
        data.put("rate", rate.value().stripTrailingZeros().toPlainString());
        data.put("effectiveDate", rate.effectiveDate().toString());
        return UtilityResult.external(data, "frankfurter", rate.payload().sourceUri(), rate.payload().retrievedAt(),
                rate.payload().cached(), false, rate.effectiveDate().toString(), Map.of());
    }

    public UtilityResult convert(Map<String, Object> arguments) {
        ToolArguments args = new ToolArguments(arguments);
        BigDecimal amount = args.decimal("amount", true);
        String base = currency(args.requiredString("baseCurrency", 3));
        String quote = currency(args.requiredString("quoteCurrency", 3));
        LocalDate date = args.date("date", null);
        int scale = args.intValue("scale", 6, 0, 18);
        RoundingMode rounding;
        try {
            rounding = RoundingMode.valueOf(args.optionalString("roundingMode", "HALF_EVEN", 32).toUpperCase(Locale.ROOT));
        }
        catch (RuntimeException ex) {
            throw UtilityToolException.invalid("roundingMode is not supported");
        }
        Rate rate = load(base, quote, date);
        BigDecimal converted = amount.multiply(rate.value()).setScale(scale, rounding);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("inputAmount", amount.toPlainString());
        data.put("baseCurrency", base);
        data.put("convertedAmount", converted.stripTrailingZeros().toPlainString());
        data.put("quoteCurrency", quote);
        data.put("rate", rate.value().stripTrailingZeros().toPlainString());
        data.put("effectiveDate", rate.effectiveDate().toString());
        data.put("scale", scale);
        data.put("roundingMode", rounding.name());
        return UtilityResult.external(data, "frankfurter", rate.payload().sourceUri(), rate.payload().retrievedAt(),
                rate.payload().cached(), false, rate.effectiveDate().toString(), Map.of());
    }

    private Rate load(String base, String quote, LocalDate date) {
        if (base.equals(quote)) {
            ProviderPayload local = new ProviderPayload(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode(),
                    java.net.URI.create("https://www.frankfurter.app/"), java.time.OffsetDateTime.now(), false);
            return new Rate(BigDecimal.ONE, date == null ? LocalDate.now(java.time.ZoneOffset.UTC) : date, local);
        }
        String path = date == null ? "/latest" : "/" + date;
        ProviderPayload payload = provider.get(path, Map.of("from", base, "to", quote));
        JsonNode value = payload.body().path("rates").get(quote);
        if (value == null || !value.isNumber()) {
            throw new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE,
                    "Frankfurter response did not contain the requested rate", true);
        }
        String effective = JsonSupport.text(payload.body(), "date");
        try {
            return new Rate(value.decimalValue(), LocalDate.parse(effective), payload);
        }
        catch (RuntimeException ex) {
            throw JsonSupport.malformed("invalid exchange-rate date");
        }
    }

    private static String currency(String value) {
        String normalized = value.toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw UtilityToolException.invalid("currency codes must be three ISO-style letters");
        }
        return normalized;
    }

    private record Rate(BigDecimal value, LocalDate effectiveDate, ProviderPayload payload) {}
}
