package org.wrj.haifa.ai.deerflow.threadfile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ThreadFileStore {

    private final ThreadFileRepository repository;

    public ThreadFileStore(ThreadFileRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ThreadFile register(String threadId, String runId, String artifactId, String path, String editablePath, String role, String status, Integer version, List<String> generatedFromWorkItemIds, List<String> generatedFromEvidenceIds) {
        ThreadFile tf = new ThreadFile();
        tf.setFileId(UUID.randomUUID().toString());
        tf.setThreadId(threadId);
        tf.setRunId(runId);
        tf.setArtifactId(artifactId);
        tf.setPath(path);
        tf.setEditablePath(editablePath);
        tf.setRole(role != null ? role : "report");
        tf.setStatus(status != null ? status : "generated");
        tf.setVersion(version != null ? version : 1);
        tf.setGeneratedFromWorkItemIds(generatedFromWorkItemIds);
        tf.setGeneratedFromEvidenceIds(generatedFromEvidenceIds);
        tf.setCreatedAt(Instant.now());
        tf.setUpdatedAt(Instant.now());
        return repository.save(tf);
    }

    @Transactional(readOnly = true)
    public List<ThreadFile> findByThreadId(String threadId) {
        return repository.findByThreadId(threadId);
    }

    @Transactional(readOnly = true)
    public List<ThreadFile> findByRunId(String runId) {
        return repository.findByRunId(runId);
    }
}
