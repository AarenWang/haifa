package org.wrj.haifa.ai.deerflow.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        List<Skill> skills = new ArrayList<>();
        if (userMessage == null || userMessage.isBlank()) {
            return skills;
        }
        Matcher m = SLASH_SKILL.matcher(userMessage);
        while (m.find()) {
            String name = m.group(1);
            Optional<Skill> skill = storage.findAny(name);
            skill.ifPresent(skills::add);
        }
        return skills;
    }
}
