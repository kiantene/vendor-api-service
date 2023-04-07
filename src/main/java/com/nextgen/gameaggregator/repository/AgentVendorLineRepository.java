package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.AgentVendorLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentVendorLineRepository extends JpaRepository<AgentVendorLine, Integer> {
    List<AgentVendorLine>  findByAgentIdAndVendorIdAndCurrencyIdAndGameCategoryId
            (Integer agentId, Integer vendorId, Integer currencyId, Integer gameCategoryId);

    List<AgentVendorLine> findByAgentIdAndVendorIdAndCurrencyId
            (Integer agentId, Integer vendorId, Integer currencyId);
}
