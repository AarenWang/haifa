package org.wrj.haifa.ai.deerflow.claim;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, String> {
    List<Claim> findByRunId(String runId);
    List<Claim> findByThreadId(String threadId);
}
