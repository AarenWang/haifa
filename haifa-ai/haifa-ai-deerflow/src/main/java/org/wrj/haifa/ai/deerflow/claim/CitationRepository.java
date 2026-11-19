package org.wrj.haifa.ai.deerflow.claim;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CitationRepository extends JpaRepository<Citation, String> {
    List<Citation> findByClaimId(String claimId);
    List<Citation> findBySourceId(String sourceId);
}
