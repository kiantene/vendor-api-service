package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.VendorPayoutSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorPayoutSettingsRepository extends JpaRepository<VendorPayoutSettings, Integer> {
    List<VendorPayoutSettings> findByMasterAgentIdAndAgentIdAndVendorIdAndGameCategoryIdAndCurrencyId(Integer masterAgentId, Integer agentId, Integer vendorId, Integer gameCategoryId, Integer currencyId);

}
