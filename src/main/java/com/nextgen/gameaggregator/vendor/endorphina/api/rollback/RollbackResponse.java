package com.nextgen.gameaggregator.vendor.endorphina.api.rollback;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RollbackResponse {

    private String transactionId;
    private BigDecimal balance;

}