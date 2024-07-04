package com.nextgen.gameaggregator.operator.sport.bet;

import com.nextgen.gameaggregator.enums.BetStatus;

import java.math.BigDecimal;
import java.util.List;

public interface SportMultipleBetData {
    String getExternalTransactionId();

    String getVendorBetId();

    String getRoundId();

    String getGameId();

    String getVendorPlayerUsername();

    BigDecimal getBetAmount();

    BigDecimal getNewBetAmount();

    BigDecimal getWinAmount();

    BigDecimal getWinLoss();

    BigDecimal getEffectiveTurnover();

    Long getVendorBetTime();

    Long getResultTime();

    Long getVendorSettleTime();

    BetStatus getBetStatus();

    Integer getBetType();

    List<SportMultipleBetIdsDto> getSportMultipleBetIdsDtoList();
}
