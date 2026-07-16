package org.wrj.haifa.ai.deerflow.sandbox;

import java.util.Comparator;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SandboxSecretRedactor {

    private static final Pattern URL_SECRET = Pattern.compile("(?i)([?&](?:token|api_key|apikey|access_token|secret)=)[^&\\s]+" );
    private final TrustedEnvironmentPolicy policy;

    public SandboxSecretRedactor(TrustedEnvironmentPolicy policy) {
        this.policy = policy;
    }

    public String redact(String value, Map<String, String> environment) {
        String result = value == null ? "" : value;
        if (environment != null) {
            var secrets = environment.entrySet().stream()
                    .filter(entry -> policy.isSensitiveName(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .filter(secret -> secret != null && secret.length() >= 4)
                    .distinct()
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .toList();
            for (String secret : secrets) {
                result = result.replace(secret, "[REDACTED]");
            }
        }
        return URL_SECRET.matcher(result).replaceAll("$1[REDACTED]");
    }
}
