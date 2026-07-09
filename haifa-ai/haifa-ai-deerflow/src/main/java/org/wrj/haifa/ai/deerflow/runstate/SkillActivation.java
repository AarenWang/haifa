package org.wrj.haifa.ai.deerflow.runstate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "deerflow_skill_activations")
public class SkillActivation {

    @Id
    @Column(name = "activation_id", length = 64, nullable = false)
    private String activationId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "skill_name", length = 128, nullable = false)
    private String skillName;

    @Column(name = "activation_reason", length = 1000)
    private String activationReason;

    @Column(name = "source", length = 64)
    private String source; // user_explicit | intent_detected | system_default | continuation

    @Column(name = "status", length = 64)
    private String status;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Lob
    @Column(name = "configuration_json")
    private String configurationJson;

    public SkillActivation() {
    }

    public String getActivationId() {
        return activationId;
    }

    public void setActivationId(String activationId) {
        this.activationId = activationId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getActivationReason() {
        return activationReason;
    }

    public void setActivationReason(String activationReason) {
        this.activationReason = activationReason;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(Instant activatedAt) {
        this.activatedAt = activatedAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public void setDeactivatedAt(Instant deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
    }

    public String getConfigurationJson() {
        return configurationJson;
    }

    public void setConfigurationJson(String configurationJson) {
        this.configurationJson = configurationJson;
    }
}
