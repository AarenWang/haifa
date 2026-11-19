package org.wrj.haifa.ai.deerflow.threadfile;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThreadFileRepository extends JpaRepository<ThreadFile, String> {
    List<ThreadFile> findByThreadId(String threadId);
    List<ThreadFile> findByRunId(String runId);
}
