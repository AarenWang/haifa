package org.wrj.haifa.ai.deerflow.sandbox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class CommandPolicy {

    private static final Set<Character> SHELL_CONTROL_CHARS = Set.of('&', ';', '|', '`', '<', '>', '\n', '\r');

    private final DeerFlowProperties properties;

    public CommandPolicy(DeerFlowProperties properties) {
        this.properties = properties;
    }

    public Decision evaluate(String command, Path workspaceRoot) {
        if (command == null || command.isBlank()) {
            return Decision.deny("command is required");
        }
        String trimmed = command.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        DeerFlowProperties.Sandbox sandbox = properties.getSandbox();

        ShellParseResult parsed = parseTokens(trimmed);
        if (!parsed.error().isBlank()) {
            return Decision.deny(parsed.error());
        }
        if (parsed.tokens().isEmpty()) {
            return Decision.deny("command executable is required");
        }

        for (String pattern : splitCsv(sandbox.getDeniedPatterns())) {
            if (!pattern.isBlank() && lower.contains(pattern.toLowerCase(Locale.ROOT))) {
                return Decision.deny("command contains denied pattern: " + pattern);
            }
        }
        for (String token : parsed.tokens()) {
            SensitivePathDecision pathDecision = evaluatePathToken(token);
            if (!pathDecision.allowed()) {
                return Decision.deny(pathDecision.reason());
            }
        }

        String executable = parsed.tokens().get(0);
        if (executable.isBlank()) {
            return Decision.deny("command executable is required");
        }
        Set<String> configuredAllowed = splitCsv(sandbox.getAllowedCommands());
        if (!configuredAllowed.isEmpty()) {
            Set<String> allowed = new java.util.HashSet<>(configuredAllowed);
            if (properties.isRunScriptEnabled()) {
                allowed.addAll(splitCsv(sandbox.getAllowedScriptLanguages()));
            }
            if (!allowed.contains(normalizeExecutable(executable))) {
                return Decision.deny("command is not in allowed command list: " + executable);
            }
        }
        return Decision.allow();
    }

    public Decision evaluateScriptBody(String code) {
        if (code == null || code.isBlank()) {
            return Decision.allow();
        }
        String lower = code.toLowerCase(Locale.ROOT);
        DeerFlowProperties.Sandbox sandbox = properties.getSandbox();

        for (String pattern : splitCsv(sandbox.getDeniedPatterns())) {
            if (!pattern.isBlank() && lower.contains(pattern.toLowerCase(Locale.ROOT))) {
                return Decision.deny("script code contains denied pattern: " + pattern);
            }
        }

        String[] sensitiveSubstrings = {
            ".git", "/etc/", "/var/", "/etc", "/var", "c:/windows", "c:\\windows",
            "%userprofile%", "$home", "~/ ", "~/"
        };
        for (String sub : sensitiveSubstrings) {
            if (lower.contains(sub)) {
                return Decision.deny("script code references a sensitive path: " + sub);
            }
        }

        if (lower.contains("..") && (lower.contains("../") || lower.contains("..\\") || lower.contains("/../") || lower.contains("\\..\\"))) {
            return Decision.deny("script code references a parent directory path");
        }

        return Decision.allow();
    }


    private static Set<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private static ShellParseResult parseTokens(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaping = false;
        for (int i = 0; i < command.length(); i++) {
            char ch = command.charAt(i);
            if (escaping) {
                token.append(ch);
                escaping = false;
                continue;
            }
            if (ch == '\\' && !inSingleQuote) {
                token.append(ch);
                escaping = true;
                continue;
            }
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && SHELL_CONTROL_CHARS.contains(ch)) {
                return new ShellParseResult(List.of(), "command contains unsupported shell control character: " + printable(ch));
            }
            if (!inSingleQuote && !inDoubleQuote && Character.isWhitespace(ch)) {
                if (!token.isEmpty()) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
                continue;
            }
            token.append(ch);
        }
        if (escaping) {
            return new ShellParseResult(List.of(), "command contains trailing escape character");
        }
        if (inSingleQuote || inDoubleQuote) {
            return new ShellParseResult(List.of(), "command contains unterminated quote");
        }
        if (!token.isEmpty()) {
            tokens.add(token.toString());
        }
        return new ShellParseResult(tokens, "");
    }

    private static String printable(char ch) {
        return switch (ch) {
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            default -> Character.toString(ch);
        };
    }

    private static SensitivePathDecision evaluatePathToken(String token) {
        String normalized = token.replace('\\', '/');
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains(".git") || lower.contains("%userprofile%") || lower.contains("$home")
                || lower.contains("/etc/") || lower.contains("/var/") || lower.equals("/etc") || lower.equals("/var")
                || lower.contains("c:/windows")) {
            return SensitivePathDecision.deny("command references a sensitive path");
        }
        if (lower.equals("~") || lower.startsWith("~/")) {
            return SensitivePathDecision.deny("command references a home directory path");
        }
        if (lower.equals("..") || lower.startsWith("../") || lower.contains("/../") || lower.endsWith("/..")) {
            return SensitivePathDecision.deny("command references a parent directory path");
        }
        if (isAbsolutePathToken(normalized)) {
            return SensitivePathDecision.deny("command references an absolute host path");
        }
        return SensitivePathDecision.allow();
    }

    private static boolean isAbsolutePathToken(String token) {
        if (token.startsWith("//") || token.startsWith("\\\\")) {
            return true;
        }
        if (token.startsWith("/") || token.startsWith("\\")) {
            return true;
        }
        return token.length() >= 3
                && Character.isLetter(token.charAt(0))
                && token.charAt(1) == ':'
                && (token.charAt(2) == '/' || token.charAt(2) == '\\');
    }

    private static String normalizeExecutable(String executable) {
        String normalized = executable.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1);
        }
        if (normalized.endsWith(".exe")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    public record Decision(boolean allowed, String reason) {
        public static Decision allow() {
            return new Decision(true, "");
        }

        public static Decision deny(String reason) {
            return new Decision(false, reason == null ? "denied" : reason);
        }
    }

    private record ShellParseResult(List<String> tokens, String error) {
    }

    private record SensitivePathDecision(boolean allowed, String reason) {
        static SensitivePathDecision allow() {
            return new SensitivePathDecision(true, "");
        }

        static SensitivePathDecision deny(String reason) {
            return new SensitivePathDecision(false, reason);
        }
    }
}
