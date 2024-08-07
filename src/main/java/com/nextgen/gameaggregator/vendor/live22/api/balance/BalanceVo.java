package com.nextgen.gameaggregator.vendor.live22.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.live22.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
public class BalanceVo extends ResponseVo {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("Balance")
    private BigDecimal balance;
}
