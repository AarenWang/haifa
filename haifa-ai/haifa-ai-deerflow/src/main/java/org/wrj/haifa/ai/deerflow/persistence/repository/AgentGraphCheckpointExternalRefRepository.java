package org.wrj.haifa.ai.deerflow.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentGraphCheckpointExternalRefEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface AgentGraphCheckpointExternalRefRepository extends JpaRepository<AgentGraphCheckpointExternalRefEntity, String> {

    @Query("select e.refId from AgentGraphCheckpointExternalRefEntity e where e.refId in :refIds")
    List<String> findExistingRefIds(@Param("refIds") Collection<String> refIds);
}
