package com.nextgen.gameaggregator.operator.sport.bet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SportMultipleBetIdsDto {
    private String betId;
    private String vendorBetId;
    private BigDecimal betAmount;
}
