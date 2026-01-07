package com.nextgen.gameaggregator.vendor.endorphina.api.bet;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class BetResponse {

    private String transactionId;
    private BigDecimal balance;

}