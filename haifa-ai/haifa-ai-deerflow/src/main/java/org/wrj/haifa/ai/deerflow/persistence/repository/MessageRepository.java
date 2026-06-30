package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.MessageEntity;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, String> {

    Optional<MessageEntity> findByMessageId(String messageId);

    List<MessageEntity> findByThreadIdOrderBySequenceNoAsc(String threadId);

    List<MessageEntity> findByRunIdOrderBySequenceNoAsc(String runId);
}
