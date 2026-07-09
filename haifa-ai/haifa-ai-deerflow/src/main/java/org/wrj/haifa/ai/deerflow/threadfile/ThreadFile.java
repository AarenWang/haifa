package org.wrj.haifa.ai.deerflow.threadfile;

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
@Table(name = "deerflow_thread_files")
public class ThreadFile {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @Column(name = "file_id", length = 64, nullable = false)
    private String fileId;

    @Column(name = "artifact_id", length = 64)
    private String artifactId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "path", length = 1024, nullable = false)
    private String path;

    @Column(name = "editable_path", length = 1024)
    private String editablePath;

    @Column(name = "role", length = 64)
    private String role; // report | code | data | image | note | intermediate

    @Column(name = "status", length = 64)
    private String status; // draft | generated | versioned | presented | archived

    @Column(name = "latest_version_of", length = 64)
    private String latestVersionOf;

    @Column(name = "version")
    private Integer version;

    @Lob
    @Column(name = "generated_from_work_item_ids_json")
    private String generatedFromWorkItemIdsJson;

    @Lob
    @Column(name = "generated_from_evidence_ids_json")
    private String generatedFromEvidenceIdsJson;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public ThreadFile() {
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getEditablePath() {
        return editablePath;
    }

    public void setEditablePath(String editablePath) {
        this.editablePath = editablePath;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLatestVersionOf() {
        return latestVersionOf;
    }

    public void setLatestVersionOf(String latestVersionOf) {
        this.latestVersionOf = latestVersionOf;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getGeneratedFromWorkItemIdsJson() {
        return generatedFromWorkItemIdsJson;
    }

    public void setGeneratedFromWorkItemIdsJson(String generatedFromWorkItemIdsJson) {
        this.generatedFromWorkItemIdsJson = generatedFromWorkItemIdsJson;
    }

    public String getGeneratedFromEvidenceIdsJson() {
        return generatedFromEvidenceIdsJson;
    }

    public void setGeneratedFromEvidenceIdsJson(String generatedFromEvidenceIdsJson) {
        this.generatedFromEvidenceIdsJson = generatedFromEvidenceIdsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> getGeneratedFromWorkItemIds() {
        if (generatedFromWorkItemIdsJson == null || generatedFromWorkItemIdsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(generatedFromWorkItemIdsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setGeneratedFromWorkItemIds(List<String> list) {
        try {
            this.generatedFromWorkItemIdsJson = MAPPER.writeValueAsString(list != null ? list : new ArrayList<>());
        } catch (Exception e) {
            this.generatedFromWorkItemIdsJson = "[]";
        }
    }

    public List<String> getGeneratedFromEvidenceIds() {
        if (generatedFromEvidenceIdsJson == null || generatedFromEvidenceIdsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(generatedFromEvidenceIdsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setGeneratedFromEvidenceIds(List<String> list) {
        try {
            this.generatedFromEvidenceIdsJson = MAPPER.writeValueAsString(list != null ? list : new ArrayList<>());
        } catch (Exception e) {
            this.generatedFromEvidenceIdsJson = "[]";
        }
    }
}
