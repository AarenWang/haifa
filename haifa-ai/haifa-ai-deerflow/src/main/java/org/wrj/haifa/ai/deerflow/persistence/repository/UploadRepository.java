package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.UploadEntity;

@Repository
public interface UploadRepository extends JpaRepository<UploadEntity, String> {

    Optional<UploadEntity> findByFileId(String fileId);

    List<UploadEntity> findByThreadIdOrderByCreatedAtAsc(String threadId);

    Optional<UploadEntity> findByFileIdAndThreadId(String fileId, String threadId);
}
