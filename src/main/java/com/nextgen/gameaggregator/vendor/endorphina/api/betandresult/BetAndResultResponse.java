package com.nextgen.gameaggregator.vendor.endorphina.api.betandresult;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class BetAndResultResponse {

    private String transactionId;
    private BigDecimal balance;

}