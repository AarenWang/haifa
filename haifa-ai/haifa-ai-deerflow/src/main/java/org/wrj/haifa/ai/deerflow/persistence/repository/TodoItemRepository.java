package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.TodoItemEntity;

@Repository
public interface TodoItemRepository extends JpaRepository<TodoItemEntity, Long> {

    List<TodoItemEntity> findByThreadIdAndRunIdOrderByOrderIndexAsc(String threadId, String runId);

    void deleteByThreadIdAndRunId(String threadId, String runId);

    @Query("select coalesce(max(t.revision), 0) from TodoItemEntity t where t.threadId = :threadId and t.runId = :runId")
    int maxRevision(@Param("threadId") String threadId, @Param("runId") String runId);
}
