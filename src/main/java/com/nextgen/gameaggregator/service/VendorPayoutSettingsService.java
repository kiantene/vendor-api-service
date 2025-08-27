package com.nextgen.gameaggregator.service;

import java.math.BigDecimal;

public interface VendorPayoutSettingsService {
    BigDecimal getMaxPayoutAmount(Integer masterAgentId, Integer agentId, Integer vendorId, Integer gameCategoryId, Integer currencyId);

}
