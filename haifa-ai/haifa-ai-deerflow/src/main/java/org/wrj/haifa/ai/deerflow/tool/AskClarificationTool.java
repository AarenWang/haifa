package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationChoice;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationQuestion;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationRecord;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AskClarificationTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String[] CHOICE_LABELS = {
            "A", "B", "C", "D", "E", "F", "G", "H"
    };
    private final ClarificationStore clarificationStore;

    public AskClarificationTool(ClarificationStore clarificationStore) {
        this.clarificationStore = clarificationStore;
    }

    @Override
    public String name() {
        return "ask_clarification";
    }

    @Override
    public String description() {
        return "Ask the user for structured clarification when information is missing or ambiguous. "
                + "Arguments: {\"title\":\"Need details\",\"clarification_type\":\"missing_info\","
                + "\"context\":\"Need this before continuing\","
                + "\"questions\":[{\"id\":\"music_style\",\"title\":\"Music style preference\","
                + "\"prompt\":\"Choose a direction or enter your own.\","
                + "\"answer_type\":\"SINGLE_CHOICE_WITH_CUSTOM\",\"allow_custom\":true,"
                + "\"choices\":[\"Energetic electronic rock\",\"Ambient folk\",\"French jazz\",\"Other\"]}]}";
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("ask_clarification");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        try {
            String jsonInput = request.userMessage();
            if (jsonInput == null || jsonInput.isBlank()) {
                return ToolResult.of(name(), "Error: arguments JSON required");
            }
            JsonNode node;
            try {
                node = MAPPER.readTree(jsonInput);
            } catch (Exception jsonEx) {
                return ToolResult.of(name(), "Error parsing tool arguments as JSON: " + jsonEx.getMessage());
            }
            String title = text(node, "title");
            String legacyQuestion = text(node, "question");
            String type = node.has("clarification_type") ? node.get("clarification_type").asText() : "missing_info";
            String context = node.has("context") ? node.get("context").asText() : "";

            List<String> options = new ArrayList<>();
            if (node.has("options") && node.get("options").isArray()) {
                for (JsonNode optionNode : node.get("options")) {
                    options.add(optionNode.asText());
                }
            }
            List<ClarificationQuestion> questions = parseQuestions(node, legacyQuestion, options);
            if (questions.isEmpty()) {
                return ToolResult.of(name(), "Error: at least one clarification question is required");
            }

            String question = firstNonBlank(title, legacyQuestion, questions.get(0).title());
            if (question == null || question.isBlank()) {
                question = "Please provide the missing details.";
            }

            ClarificationRecord record = clarificationStore.create(
                    request.threadId(),
                    request.runId(),
                    question,
                    type,
                    context,
                    options,
                    questions
            );

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("clarificationRequired", true);
            metadata.put("clarificationId", record.clarificationId());
            metadata.put("question", question);
            metadata.put("clarificationType", type);
            metadata.put("options", options);
            metadata.put("questions", questions);

            return ToolResult.of(
                    name(),
                    "Clarification requested from user:\nQuestion: " + question,
                    metadata
            );
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }

    private static List<ClarificationQuestion> parseQuestions(JsonNode node, String legacyQuestion, List<String> legacyOptions) {
        List<ClarificationQuestion> questions = new ArrayList<>();
        if (node.has("questions") && node.get("questions").isArray()) {
            int index = 0;
            for (JsonNode questionNode : node.get("questions")) {
                ClarificationQuestion question = parseQuestion(questionNode, ++index);
                if (question != null) {
                    questions.add(question);
                }
            }
        }
        if (questions.isEmpty() && legacyQuestion != null && !legacyQuestion.isBlank()) {
            List<ClarificationChoice> choices = normalizeChoices(legacyOptions);
            String answerType = choices.isEmpty() ? "TEXT" : "SINGLE_CHOICE_WITH_CUSTOM";
            questions.add(new ClarificationQuestion(
                    "q1",
                    stripMarkdownHeading(legacyQuestion),
                    legacyQuestion,
                    answerType,
                    choices,
                    true,
                    true
            ));
        }
        return questions;
    }

    private static ClarificationQuestion parseQuestion(JsonNode node, int index) {
        if (node == null || !node.isObject()) {
            return null;
        }
        String id = firstNonBlank(text(node, "id"), "q" + index);
        String title = firstNonBlank(text(node, "title"), text(node, "question"), "Question " + index);
        String prompt = firstNonBlank(text(node, "prompt"), text(node, "description"), title);
        List<ClarificationChoice> choices = normalizeChoices(node.get("choices"));
        String answerType = firstNonBlank(text(node, "answer_type"), text(node, "answerType"));
        if (answerType == null) {
            answerType = choices.isEmpty() ? "TEXT" : "SINGLE_CHOICE_WITH_CUSTOM";
        }
        boolean allowCustom = choices.isEmpty()
                || booleanValue(node, "allow_custom", booleanValue(node, "allowCustom", false));
        boolean required = booleanValue(node, "required", true);
        return new ClarificationQuestion(
                id,
                title,
                prompt,
                answerType,
                choices,
                allowCustom,
                required
        );
    }

    private static List<ClarificationChoice> normalizeChoices(JsonNode choicesNode) {
        List<String> values = new ArrayList<>();
        if (choicesNode != null && choicesNode.isArray()) {
            for (JsonNode choiceNode : choicesNode) {
                if (choiceNode.isTextual()) {
                    values.add(choiceNode.asText());
                } else if (choiceNode.isObject()) {
                    String text = firstNonBlank(text(choiceNode, "text"), text(choiceNode, "label"), text(choiceNode, "value"));
                    if (text != null && !text.isBlank()) {
                        values.add(text);
                    }
                }
            }
        }
        return normalizeChoices(values);
    }

    private static List<ClarificationChoice> normalizeChoices(List<String> choices) {
        if (choices == null || choices.isEmpty()) {
            return List.of();
        }
        List<ClarificationChoice> result = new ArrayList<>();
        int index = 0;
        for (String rawChoice : choices) {
            if (rawChoice == null || rawChoice.isBlank()) {
                continue;
            }
            String label = index < CHOICE_LABELS.length ? CHOICE_LABELS[index] : Integer.toString(index + 1);
            result.add(new ClarificationChoice(label.toLowerCase(), label, rawChoice.trim()));
            index++;
        }
        return result;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        String value = node.get(field).asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean booleanValue(JsonNode node, String field, boolean defaultValue) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return defaultValue;
        }
        return node.get(field).asBoolean(defaultValue);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String stripMarkdownHeading(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("*", "").replace("#", "").trim();
    }
}
