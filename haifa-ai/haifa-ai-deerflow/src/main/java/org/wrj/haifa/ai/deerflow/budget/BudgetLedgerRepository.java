package org.wrj.haifa.ai.deerflow.budget;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetLedgerRepository extends JpaRepository<BudgetLedger, String> {
    Optional<BudgetLedger> findByRunId(String runId);
}
