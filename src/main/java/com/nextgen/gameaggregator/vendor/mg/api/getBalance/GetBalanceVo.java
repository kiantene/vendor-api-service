package com.nextgen.gameaggregator.vendor.mg.api.getBalance;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetBalanceVo {
    private String currency;
    private BigDecimal balance;
}
