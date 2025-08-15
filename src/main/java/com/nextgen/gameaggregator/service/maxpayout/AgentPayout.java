package com.nextgen.gameaggregator.service.maxpayout;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AgentPayout {
    private BigDecimal originalWinAmount;
    private BigDecimal winAmount;
    private BigDecimal winAmountDifference;

    public AgentPayout(BigDecimal originalWinAmount, BigDecimal winAmount) {
        this.originalWinAmount = originalWinAmount;
        this.winAmount = winAmount;
        if (winAmount.compareTo(originalWinAmount) < 0) {
            winAmountDifference = originalWinAmount.subtract(winAmount);
        } else {
            winAmountDifference = BigDecimal.ZERO;
        }
    }
}
