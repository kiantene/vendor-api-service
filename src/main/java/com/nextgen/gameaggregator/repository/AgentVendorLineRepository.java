package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.AgentVendorLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentVendorLineRepository extends JpaRepository<AgentVendorLine, Integer> {
    AgentVendorLine findByAgentIdAndVendorIdAndCurrencyId(Integer agentId, Integer vendorId, Integer currencyId);
}
