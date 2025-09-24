package com.nextgen.gameaggregator.operator.wallet.settled;

import com.nextgen.gameaggregator.enums.BetStatus;

import java.math.BigDecimal;

public interface BetResultData {
    String getExternalTransactionId();
    String getVendorBetId();
    String getRoundId();
    String getGameId();
    BigDecimal getBetAmount();
    BigDecimal getWinAmount();
    BigDecimal getWinLoss();
    BigDecimal getEffectiveTurnover();

    //    ResultType getResultType();
    Long getVendorBetTime();
    Long getResultTime();
    Long getVendorSettleTime();
    BigDecimal getJackpotAmount();
    Integer getIsFreespin();
    BetStatus getBetStatus();
    default Integer isEndRound() {
        return null;
    }
    default boolean getShouldSettleByBet() {
        return false;
    }

    /**
     * Logic for new framework to support backward compatibility
     */
    default boolean isNewFramework() { return false; }
    default String getGaBetId() { return null; }
}
