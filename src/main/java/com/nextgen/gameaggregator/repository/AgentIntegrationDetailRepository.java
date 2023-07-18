package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.AgentIntegrationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentIntegrationDetailRepository extends JpaRepository<AgentIntegrationDetail, Integer> {
}
