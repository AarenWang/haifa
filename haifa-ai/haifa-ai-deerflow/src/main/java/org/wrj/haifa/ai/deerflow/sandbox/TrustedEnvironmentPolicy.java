package org.wrj.haifa.ai.deerflow.sandbox;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class TrustedEnvironmentPolicy {

    private static final Set<String> DEFAULT_DENIED_NAMES = Set.of(
            "OPENAI_API_KEY", "OPENROUTER_API_KEY", "ANTHROPIC_API_KEY", "ANTHROPIC_TOKEN",
            "GOOGLE_API_KEY", "GEMINI_API_KEY", "MINIMAX_API_KEY", "DASHSCOPE_API_KEY",
            "GH_TOKEN", "GITHUB_TOKEN", "GITHUB_APP_PRIVATE_KEY", "GITHUB_APP_PRIVATE_KEY_PATH",
            "AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY", "AWS_SESSION_TOKEN",
            "AZURE_CLIENT_SECRET", "GOOGLE_APPLICATION_CREDENTIALS",
            "SLACK_BOT_TOKEN", "SLACK_APP_TOKEN", "TELEGRAM_BOT_TOKEN", "DISCORD_BOT_TOKEN",
            "JWT_SECRET", "SESSION_SECRET", "ENCRYPTION_KEY", "SPRING_DATASOURCE_PASSWORD");

    private static final List<Pattern> DEFAULT_DENIED_PATTERNS = List.of(
            Pattern.compile("(?i).*(?:_API_KEY|_TOKEN|_SECRET|_PASSWORD|_PASSWD|_PRIVATE_KEY|_CREDENTIALS?|_ACCESS_KEY|_SESSION_KEY)$"),
            Pattern.compile("(?i)^(?:SPRING_DATASOURCE|DATABASE|DB)_.*(?:PASSWORD|SECRET|TOKEN).*$"));

    private static final Set<String> NEVER_PASSTHROUGH = Set.of(
            "JWT_SECRET", "SESSION_SECRET", "ENCRYPTION_KEY", "DEERFLOW_ADMIN_TOKEN");

    private final DeerFlowProperties properties;

    public TrustedEnvironmentPolicy(DeerFlowProperties properties) {
        this.properties = properties;
    }

    public boolean shouldInherit(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = normalize(name);
        Set<String> passthrough = normalizedSet(properties.getSandbox().getLocalTrusted().getPassthroughEnvironmentNames());
        if (passthrough.contains(normalized) && !NEVER_PASSTHROUGH.contains(normalized)) {
            return true;
        }
        Set<String> denied = new HashSet<>(DEFAULT_DENIED_NAMES);
        denied.addAll(normalizedSet(properties.getSandbox().getLocalTrusted().getDeniedEnvironmentNames()));
        if (denied.contains(normalized)) {
            return false;
        }
        for (Pattern pattern : DEFAULT_DENIED_PATTERNS) {
            if (pattern.matcher(name).matches()) {
                return false;
            }
        }
        for (String configured : properties.getSandbox().getLocalTrusted().getDeniedEnvironmentPatterns()) {
            if (configured != null && !configured.isBlank() && Pattern.compile(configured, Pattern.CASE_INSENSITIVE).matcher(name).matches()) {
                return false;
            }
        }
        return true;
    }

    public boolean isSensitiveName(String name) {
        return !shouldInherit(name) || DEFAULT_DENIED_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(name == null ? "" : name).matches());
    }

    private static Set<String> normalizedSet(List<String> values) {
        Set<String> result = new HashSet<>();
        if (values != null) {
            values.stream().filter(value -> value != null && !value.isBlank()).map(TrustedEnvironmentPolicy::normalize).forEach(result::add);
        }
        return result;
    }

    private static String normalize(String value) {
        return value.toUpperCase(Locale.ROOT);
    }
}
