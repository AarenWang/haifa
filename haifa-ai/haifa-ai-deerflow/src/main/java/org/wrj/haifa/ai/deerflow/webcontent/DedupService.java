package org.wrj.haifa.ai.deerflow.webcontent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

@Component
public class DedupService {

    public String contentHash(String content) {
        String normalized = normalizeText(content);
        if (normalized.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(encoded.length * 2);
            for (byte value : encoded) {
                sb.append(String.format("%02x", value));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public String normalizeText(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        return content
                .replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .replaceAll("(?i)cookie preferences|accept all cookies|privacy policy|terms of service", " ")
                .trim()
                .toLowerCase();
    }
}
