package com.nextgen.gameaggregator.service.data.producer;

import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;

import java.math.BigDecimal;
import java.util.List;

public record BetHistoryPublishContext(String productCode,
                                       Integer productId,
                                       Integer productGameId,
                                       String agentPlayerUsername,
                                       String vendorPlayerUsername,
                                       BigDecimal fromVendorRate,
                                       boolean requirePreprocessing,
                                       List<BetTransaction> txnList) {
}
