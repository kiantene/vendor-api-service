package com.nextgen.gameaggregator.vendor.endorphina.api.result;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BetResultResponse {

    private String transactionId;
    private BigDecimal balance;

}