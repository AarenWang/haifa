package org.wrj.haifa.ai.deerflow.skill;

import java.util.List;
import java.util.Optional;

public interface SkillStorage {

    List<Skill> listPublicSkills();

    List<Skill> listCustomSkills();

    Optional<Skill> findPublic(String name);

    Optional<Skill> findCustom(String name);

    default Optional<Skill> findAny(String name) {
        Optional<Skill> custom = findCustom(name);
        if (custom.isPresent()) {
            return custom;
        }
        return findPublic(name);
    }

    default List<Skill> listAll() {
        List<Skill> all = new java.util.ArrayList<>(listPublicSkills());
        all.addAll(listCustomSkills());
        return all;
    }
}
