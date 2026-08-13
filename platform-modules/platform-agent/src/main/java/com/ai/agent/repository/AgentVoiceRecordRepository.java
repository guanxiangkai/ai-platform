package com.ai.agent.repository;

import com.ai.agent.domain.entity.AgentVoiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** 语音转写审计持久化接口。 */
public interface AgentVoiceRecordRepository extends JpaRepository<AgentVoiceRecord, String>,
        JpaSpecificationExecutor<AgentVoiceRecord> {
}
