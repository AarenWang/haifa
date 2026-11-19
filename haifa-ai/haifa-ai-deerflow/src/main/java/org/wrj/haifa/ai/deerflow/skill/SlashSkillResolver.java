package org.wrj.haifa.ai.deerflow.skill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class SlashSkillResolver {

    private static final Pattern SLASH_SKILL = Pattern.compile("(?<=^|\\s)/([a-zA-Z0-9_-]+)(?=\\s|$)");

    private final SkillStorage storage;

    public SlashSkillResolver(SkillStorage storage) {
        this.storage = storage;
    }

    public List<Skill> resolve(String userMessage) {
        Map<String, Skill> skills = new LinkedHashMap<>();
        if (userMessage == null || userMessage.isBlank()) {
            return List.of();
        }
        Matcher m = SLASH_SKILL.matcher(userMessage);
        while (m.find()) {
            String name = m.group(1);
            storage.findAny(name).ifPresent(skill -> skills.put(skill.name(), skill));
        }
        for (Skill skill : storage.listAll()) {
            if (!skills.containsKey(skill.name()) && matchesActivationHints(userMessage, skill)) {
                skills.put(skill.name(), skill);
            }
        }
        return new ArrayList<>(skills.values());
    }

    private static boolean matchesActivationHints(String userMessage, Skill skill) {
        if (skill.activationHints() == null || skill.activationHints().isEmpty()) {
            return false;
        }
        String normalizedMessage = normalize(userMessage);
        for (String hint : skill.activationHints()) {
            String normalizedHint = normalize(hint);
            if (!normalizedHint.isBlank() && normalizedMessage.contains(normalizedHint)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
