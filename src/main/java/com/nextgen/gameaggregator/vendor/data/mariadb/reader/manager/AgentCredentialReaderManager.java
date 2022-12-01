package com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager;

import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.AgentCredentialReader;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentCredentialReaderManager extends JpaRepository<AgentCredentialReader, Long> {

    AgentCredentialReader findByAgentId (Long agentId);
}
