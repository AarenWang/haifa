package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.ThreadEntity;

@Repository
public interface ThreadRepository extends JpaRepository<ThreadEntity, String> {

    List<ThreadEntity> findAllByOrderByUpdatedAtDesc();

    Optional<ThreadEntity> findByThreadId(String threadId);
}
