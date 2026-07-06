package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationStatus;

@Entity
@Table(name = "deerflow_clarifications", indexes = {
        @Index(name = "idx_clarifications_thread_status", columnList = "thread_id, status"),
        @Index(name = "idx_clarifications_run_id", columnList = "run_id")
})
public class ClarificationEntity {

    @Id
    @Column(name = "clarification_id", length = 64, nullable = false)
    private String clarificationId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "question", length = 4000)
    private String question;

    @Column(name = "clarification_type", length = 64)
    private String clarificationType;

    @Column(name = "context", length = 4000)
    private String context;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private ClarificationStatus status;

    @Column(name = "answer", length = 4000)
    private String answer;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @Column(name = "options_json", length = 20000)
    private String optionsJson;

    @Column(name = "questions_json", length = 20000)
    private String questionsJson;

    @Column(name = "answers_json", length = 20000)
    private String answersJson;

    public String getClarificationId() {
        return clarificationId;
    }

    public void setClarificationId(String clarificationId) {
        this.clarificationId = clarificationId;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getClarificationType() {
        return clarificationType;
    }

    public void setClarificationType(String clarificationType) {
        this.clarificationType = clarificationType;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public ClarificationStatus getStatus() {
        return status;
    }

    public void setStatus(ClarificationStatus status) {
        this.status = status;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public String getQuestionsJson() {
        return questionsJson;
    }

    public void setQuestionsJson(String questionsJson) {
        this.questionsJson = questionsJson;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public void setAnswersJson(String answersJson) {
        this.answersJson = answersJson;
    }
}
