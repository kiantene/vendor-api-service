package com.nextgen.gameaggregator.operator.wallet.settled;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.WinType;
import java.math.BigDecimal;

public interface UnsettledResultSettledData {
    String getExternalTransactionId();
    String getVendorBetId();
    String getRoundId();
    String getGameId();
    BigDecimal getBetAmount();
    BigDecimal getWinAmount();
    BigDecimal getWinLoss();
    BigDecimal getEffectiveTurnover();
    BigDecimal getRefundAmount();
    WinType getResultType();
    Long getVendorBetTime();
    Long getResultTime();
    Long getVendorSettleTime();
    BigDecimal getJackpotAmount();
    Integer getIsFreespin();
    BetStatus getBetStatus();
}
