package com.nextgen.gameaggregator.operator.wallet.settled;

import com.nextgen.gameaggregator.enums.WinType;
import java.math.BigDecimal;

public interface UnsettledResultSettledData {
    String getExternalTransactionId();
    String getBetId();
    String getRoundId();
    String getGameId();
    BigDecimal getBetAmount();
    BigDecimal getWinAmount();
    BigDecimal getWinLoss();
    BigDecimal getVendorWinLoss();
    BigDecimal getEffectiveTurnover();
    BigDecimal getRefundAmount();
    WinType getResultType();
    Long getVendorBetTime();
    Long getResultTime();
    Long getVendorSettleTime();
}
