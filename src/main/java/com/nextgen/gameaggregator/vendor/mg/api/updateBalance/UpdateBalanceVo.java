package com.nextgen.gameaggregator.vendor.mg.api.updateBalance;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateBalanceVo {
    private String extTxnId;
    private String currency;
    private BigDecimal balance;
    private Long extCreationTimeMs;
}