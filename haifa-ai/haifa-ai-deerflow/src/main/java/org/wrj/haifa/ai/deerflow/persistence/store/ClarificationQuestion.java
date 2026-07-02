package org.wrj.haifa.ai.deerflow.persistence.store;

import java.util.List;

public record ClarificationQuestion(
        String id,
        String title,
        String prompt,
        String answerType,
        List<ClarificationChoice> choices,
        boolean allowCustom,
        boolean required
) {
}
