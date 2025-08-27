package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.VendorPayoutSettings;
import com.nextgen.gameaggregator.repository.ga.writer.VendorPayoutSettingsRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class VendorPayoutSettingsServiceImpl implements VendorPayoutSettingsService {

    private final VendorPayoutSettingsRepository vendorPayoutSettingsRepository;

    public VendorPayoutSettingsServiceImpl(VendorPayoutSettingsRepository vendorPayoutSettingsRepository) {
        this.vendorPayoutSettingsRepository = vendorPayoutSettingsRepository;
    }

    @Override
    public BigDecimal getMaxPayoutAmount(Integer masterAgentId,
                                         Integer agentId,
                                         Integer vendorId,
                                         Integer gameCategoryId,
                                         Integer currencyId) {

        BigDecimal maxPayoutAmount = null;

        //First step is to check by agent level.
        List<VendorPayoutSettings> vendorPayoutSettingsList = vendorPayoutSettingsRepository.findByMasterAgentIdAndAgentIdAndVendorIdAndGameCategoryIdAndCurrencyId(masterAgentId, agentId, vendorId, gameCategoryId, currencyId);

        maxPayoutAmount = vendorPayoutSettingsList.stream()
                .filter(v -> v.getStatus() != null && v.getStatus() == 1)
                .max(Comparator.comparing(VendorPayoutSettings::getVersion))
                .map(VendorPayoutSettings::getMaxPayout)
                .orElse(null);

        if (maxPayoutAmount == null) {
            //If maxPayoutAmount is not found by agent level, then check from masterAgent level.
            vendorPayoutSettingsList = vendorPayoutSettingsRepository.findByMasterAgentIdAndAgentIdAndVendorIdAndGameCategoryIdAndCurrencyId(masterAgentId, 0, vendorId, gameCategoryId, currencyId);

            maxPayoutAmount = vendorPayoutSettingsList.stream()
                    .filter(v -> v.getStatus() != null && v.getStatus() == 1)
                    .max(Comparator.comparing(VendorPayoutSettings::getVersion))
                    .map(VendorPayoutSettings::getMaxPayout)
                    .orElse(null);
        }

        return maxPayoutAmount;
    }
}
