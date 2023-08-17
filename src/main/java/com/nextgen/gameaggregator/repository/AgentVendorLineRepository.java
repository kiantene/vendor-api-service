package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.AgentVendorLine;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentVendorLineRepository extends JpaRepository<AgentVendorLine, Integer> {
    List<AgentVendorLine>  findByAgentIdAndVendorIdAndCurrencyIdAndGameCategoryId
            (Integer agentId, Integer vendorId, Integer currencyId, Integer gameCategoryId);

    List<AgentVendorLine> findByAgentIdAndVendorIdAndCurrencyId
            (Integer agentId, Integer vendorId, Integer currencyId);

    List<AgentVendorLine> findByVendorLineId(Integer vendorLineId);

    List<AgentVendorLine> findByAgentIdAndVendorLineId(Integer agentId, Integer vendorLineId);

    @Cacheable(value = "AgentVendorLine", key = "{#agentId, #vendorId, #currencyId, #gameCategoryId, #status}", cacheManager = "cacheManager")
    List<AgentVendorLine>  findByAgentIdAndVendorIdAndCurrencyIdAndGameCategoryIdAndStatus
            (Integer agentId, Integer vendorId, Integer currencyId, Integer gameCategoryId, Integer status);
}
