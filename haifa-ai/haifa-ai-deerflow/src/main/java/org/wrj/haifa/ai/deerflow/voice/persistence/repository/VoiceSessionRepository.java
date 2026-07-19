package org.wrj.haifa.ai.deerflow.voice.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.voice.persistence.entity.VoiceSessionEntity;

@Repository
public interface VoiceSessionRepository extends JpaRepository<VoiceSessionEntity, String> {
}
