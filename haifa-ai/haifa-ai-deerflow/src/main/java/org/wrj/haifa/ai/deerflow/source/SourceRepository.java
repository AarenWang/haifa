package org.wrj.haifa.ai.deerflow.source;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SourceRepository extends JpaRepository<Source, String> {
    List<Source> findByRunId(String runId);
    List<Source> findByThreadId(String threadId);
    Optional<Source> findByUrlAndThreadId(String url, String threadId);
    Optional<Source> findByUrlAndThreadIdAndRunId(String url, String threadId, String runId);
}
