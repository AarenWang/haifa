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
    private static final String STATEMENT_BOUNDARY = "\u0000";

    private final DeerFlowProperties properties;

    public CommandPolicy(DeerFlowProperties properties) {
        this.properties = properties;
    }

    public Decision evaluate(String command, Path workspaceRoot) {
        if (command == null || command.isBlank()) {
            return Decision.deny("command is required");
        }
        String trimmed = command.trim();
        if (trimmed.indexOf('|') >= 0) {
            return Decision.deny("command contains disabled pipe character: |");
        }
        DeerFlowProperties.Sandbox sandbox = properties.getSandbox();

        ShellParseResult parsed = parseTokens(trimmed);
        if (!parsed.error().isBlank()) {
            return Decision.deny(parsed.error());
        }
        if (parsed.tokens().isEmpty()) {
            return Decision.deny("command executable is required");
        }

        for (String pattern : splitCsv(sandbox.getDeniedPatterns())) {
            if (matchesCommandRule(parsed.tokens(), pattern)) {
                return Decision.deny("command matches denied command rule: " + pattern);
            }
        }
        for (String token : parsed.tokens()) {
            SensitivePathDecision pathDecision = evaluatePathToken(token, properties.getSkillsContainerPath());
            if (!pathDecision.allowed()) {
                return Decision.deny(pathDecision.reason());
            }
        }

        String executable = parsed.tokens().get(0);
        if (executable.isBlank()) {
            return Decision.deny("command executable is required");
        }
        if (executable.indexOf('/') >= 0 || executable.indexOf('\\') >= 0) {
            return Decision.deny("command executable must be a bare command name: " + executable);
        }
        Set<String> configuredAllowed = splitCsv(sandbox.getAllowedCommands());
        if (configuredAllowed.isEmpty()) {
            return Decision.deny("allowed command list is empty");
        }
        Set<String> allowed = configuredAllowed.stream()
                .map(CommandPolicy::normalizeExecutable)
                .collect(Collectors.toCollection(java.util.HashSet::new));
        if (properties.isRunScriptEnabled()) {
            splitCsv(sandbox.getAllowedScriptLanguages()).stream()
                    .map(CommandPolicy::normalizeExecutable)
                    .forEach(allowed::add);
        }
        if (!allowed.contains(normalizeExecutable(executable))) {
            return Decision.deny("command is not in allowed command list: " + executable);
        }
        return Decision.allow();
    }

    public Decision evaluateScriptBody(String code) {
        if (code == null || code.isBlank()) {
            return Decision.allow();
        }
        if (code.indexOf('|') >= 0) {
            return Decision.deny("script code contains disabled pipe character: |");
        }
        String lower = code.toLowerCase(Locale.ROOT);
        DeerFlowProperties.Sandbox sandbox = properties.getSandbox();

        for (String pattern : splitCsv(sandbox.getDeniedPatterns())) {
            if (matchesScriptRule(code, pattern)) {
                return Decision.deny("script code matches denied command rule: " + pattern);
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

    private static boolean matchesCommandRule(List<String> commandTokens, String rule) {
        List<String> ruleTokens = securityTokens(rule);
        if (commandTokens == null || commandTokens.isEmpty() || ruleTokens.isEmpty()) {
            return false;
        }
        List<String> normalized = commandTokens.stream()
                .map(CommandPolicy::normalizeSecurityToken)
                .toList();
        if (!normalizeExecutable(normalized.get(0)).equals(normalizeExecutable(ruleTokens.get(0)))) {
            return false;
        }
        if (ruleTokens.size() == 1) {
            return true;
        }
        return containsOrderedRuleArguments(normalized, 1, ruleTokens);
    }

    private static boolean matchesScriptRule(String code, String rule) {
        List<String> scriptTokens = securityTokens(code);
        List<String> ruleTokens = securityTokens(rule);
        if (ruleTokens.isEmpty()) {
            return false;
        }
        for (int index = 0; index < scriptTokens.size(); index++) {
            if (!normalizeSecurityToken(scriptTokens.get(index)).equals(normalizeSecurityToken(ruleTokens.get(0)))) {
                continue;
            }
            if (ruleTokens.size() == 1 || containsOrderedRuleArguments(scriptTokens, index + 1, ruleTokens)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsOrderedRuleArguments(List<String> source, int sourceStart, List<String> rule) {
        int ruleIndex = 1;
        for (int index = sourceStart; index < source.size(); index++) {
            String actual = source.get(index);
            if (STATEMENT_BOUNDARY.equals(actual)) {
                return false;
            }
            if (normalizeSecurityToken(actual).equals(normalizeSecurityToken(rule.get(ruleIndex)))) {
                ruleIndex++;
                if (ruleIndex == rule.size()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Extracts case-insensitive security tokens while ignoring quoted strings,
     * PowerShell variables, and line/block comments. This deliberately avoids
     * the unsafe substring matching that treated secureboot as reboot and
     * NoTypeInformation as format.
     */
    private static List<String> securityTokens(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean lineComment = false;
        boolean blockComment = false;
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            char next = i + 1 < value.length() ? value.charAt(i + 1) : '\0';
            if (lineComment) {
                if (ch == '\n' || ch == '\r') {
                    lineComment = false;
                    addStatementBoundary(tokens);
                }
                continue;
            }
            if (blockComment) {
                if (ch == '#' && next == '>') {
                    blockComment = false;
                    i++;
                }
                continue;
            }
            if (!singleQuoted && !doubleQuoted && ch == '<' && next == '#') {
                flushSecurityToken(tokens, token);
                blockComment = true;
                i++;
                continue;
            }
            if (!singleQuoted && !doubleQuoted && ch == '#') {
                flushSecurityToken(tokens, token);
                lineComment = true;
                continue;
            }
            if (escaped) {
                escaped = false;
                continue;
            }
            if (doubleQuoted && ch == '`') {
                escaped = true;
                continue;
            }
            if (ch == '\'' && !doubleQuoted) {
                flushSecurityToken(tokens, token);
                singleQuoted = !singleQuoted;
                continue;
            }
            if (ch == '"' && !singleQuoted) {
                flushSecurityToken(tokens, token);
                doubleQuoted = !doubleQuoted;
                continue;
            }
            if (singleQuoted || doubleQuoted) {
                continue;
            }
            if (ch == '$') {
                flushSecurityToken(tokens, token);
                while (i + 1 < value.length() && isSecurityTokenChar(value.charAt(i + 1))) {
                    i++;
                }
                continue;
            }
            if (isSecurityTokenChar(ch)) {
                token.append(Character.toLowerCase(ch));
            } else {
                flushSecurityToken(tokens, token);
                if (ch == ';' || ch == '\n' || ch == '\r') {
                    addStatementBoundary(tokens);
                }
            }
        }
        flushSecurityToken(tokens, token);
        return tokens;
    }

    private static boolean isSecurityTokenChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '/' || ch == '.';
    }

    private static void flushSecurityToken(List<String> tokens, StringBuilder token) {
        if (!token.isEmpty()) {
            tokens.add(normalizeSecurityToken(token.toString()));
            token.setLength(0);
        }
    }

    private static void addStatementBoundary(List<String> tokens) {
        if (!tokens.isEmpty() && !STATEMENT_BOUNDARY.equals(tokens.get(tokens.size() - 1))) {
            tokens.add(STATEMENT_BOUNDARY);
        }
    }

    private static String normalizeSecurityToken(String token) {
        String lower = token == null ? "" : token.toLowerCase(Locale.ROOT);
        return normalizeExecutable(lower);
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

    private static SensitivePathDecision evaluatePathToken(String token, String skillsContainerPath) {
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
        if (isAbsolutePathToken(normalized) && !isAllowedVirtualPath(normalized, skillsContainerPath)) {
            return SensitivePathDecision.deny("command references an absolute host path");
        }
        return SensitivePathDecision.allow();
    }

    private static boolean isAllowedVirtualPath(String token, String skillsContainerPath) {
        String path = stripArgumentPrefix(token).replace('\\', '/');
        String skillsRoot = skillsContainerPath == null || skillsContainerPath.isBlank()
                ? "/mnt/skills" : skillsContainerPath.replace('\\', '/').replaceAll("/$", "");
        return path.equals(skillsRoot) || path.startsWith(skillsRoot + "/")
                || path.equals("/mnt/user-data/uploads") || path.startsWith("/mnt/user-data/uploads/")
                || path.equals("/mnt/user-data/workspace") || path.startsWith("/mnt/user-data/workspace/")
                || path.equals("/mnt/user-data/outputs") || path.startsWith("/mnt/user-data/outputs/");
    }

    private static String stripArgumentPrefix(String token) {
        int equals = token.indexOf('=');
        return equals >= 0 ? token.substring(equals + 1) : token;
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
        for (String extension : List.of(".exe", ".cmd", ".bat", ".com")) {
            if (normalized.endsWith(extension)) {
                normalized = normalized.substring(0, normalized.length() - extension.length());
                break;
            }
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
