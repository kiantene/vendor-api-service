package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.AgentPayoutSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentPayoutSettingRepository extends JpaRepository<AgentPayoutSetting, Integer> {
    List<AgentPayoutSetting> findByMasterAgentIdAndVendorIdAndGameCategoryIdAndCurrencyId(Integer masterAgentId, Integer vendorId, Integer gameCategoryId, Integer currencyId);

}
