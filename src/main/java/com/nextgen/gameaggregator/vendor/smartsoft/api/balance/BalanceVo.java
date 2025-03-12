package com.nextgen.gameaggregator.vendor.smartsoft.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.smartsoft.vo.ResponseVo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceVo extends ResponseVo {
    @JsonProperty("Amount")
    private BigDecimal amount;

    @JsonProperty("CurrencyCode")
    private String currencyCode;
}