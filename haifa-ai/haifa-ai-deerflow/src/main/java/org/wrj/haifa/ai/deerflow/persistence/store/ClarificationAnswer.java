package org.wrj.haifa.ai.deerflow.persistence.store;

import java.util.List;

public record ClarificationAnswer(
        String questionId,
        String answer,
        List<String> selectedChoiceIds,
        String customAnswer
) {
}
