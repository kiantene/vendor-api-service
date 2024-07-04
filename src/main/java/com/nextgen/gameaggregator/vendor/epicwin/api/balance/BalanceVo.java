package com.nextgen.gameaggregator.vendor.epicwin.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.epicwin.vo.ResponseVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceVo extends ResponseVo {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("Balance")
    private BigDecimal balance;
}
