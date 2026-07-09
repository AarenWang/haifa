package org.wrj.haifa.ai.deerflow.quality;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "deerflow_quality_assessments")
public class QualityAssessment {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @Column(name = "assessment_id", length = 64, nullable = false)
    private String assessmentId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "score")
    private Double score;

    @Column(name = "passed")
    private Boolean passed;

    @Lob
    @Column(name = "gaps_json")
    private String gapsJson;

    @Lob
    @Column(name = "risks_json")
    private String risksJson;

    @Column(name = "next_action", length = 64)
    private String nextAction; // continue | ask_user | produce_partial | synthesize | fail

    @Lob
    @Column(name = "limitations")
    private String limitations;

    @Column(name = "created_at")
    private Instant createdAt;

    public QualityAssessment() {
    }

    public String getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(String assessmentId) {
        this.assessmentId = assessmentId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public String getGapsJson() {
        return gapsJson;
    }

    public void setGapsJson(String gapsJson) {
        this.gapsJson = gapsJson;
    }

    public String getRisksJson() {
        return risksJson;
    }

    public void setRisksJson(String risksJson) {
        this.risksJson = risksJson;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    public String getLimitations() {
        return limitations;
    }

    public void setLimitations(String limitations) {
        this.limitations = limitations;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getGaps() {
        if (gapsJson == null || gapsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(gapsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setGaps(List<String> list) {
        try {
            this.gapsJson = MAPPER.writeValueAsString(list != null ? list : new ArrayList<>());
        } catch (Exception e) {
            this.gapsJson = "[]";
        }
    }

    public List<String> getRisks() {
        if (risksJson == null || risksJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(risksJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setRisks(List<String> list) {
        try {
            this.risksJson = MAPPER.writeValueAsString(list != null ? list : new ArrayList<>());
        } catch (Exception e) {
            this.risksJson = "[]";
        }
    }
}
