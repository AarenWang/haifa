package org.wrj.haifa.ai.deerflow.sandbox;

import java.nio.file.Path;
import java.util.Locale;

final class DockerPathMapper {

    private DockerPathMapper() {
    }

    static String bindSource(Path hostPath) {
        Path normalizedPath = hostPath.toAbsolutePath().normalize();
        return bindSource(normalizedPath.toString());
    }

    static String bindSource(String hostPath) {
        String normalized = hostPath.replace('\\', '/');
        if (isWindowsPath(normalized)) {
            char drive = Character.toLowerCase(normalized.charAt(0));
            String rest = normalized.substring(2);
            if (!rest.startsWith("/")) {
                rest = "/" + rest;
            }
            return "//" + drive + rest;
        }
        return normalized;
    }

    private static boolean isWindowsPath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.length() >= 3
                && lower.charAt(1) == ':'
                && lower.charAt(2) == '/'
                && lower.charAt(0) >= 'a'
                && lower.charAt(0) <= 'z';
    }
}
