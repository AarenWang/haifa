package org.wrj.haifa.ai.deerflow.work;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, String> {
    List<WorkItem> findByRunId(String runId);
    List<WorkItem> findByThreadId(String threadId);
}
