package com.nextgen.gameaggregator.vendor.dblive.api.betcancel;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BetCancelDataVo {
    private BigDecimal balance;
    private BigDecimal rollbackAmount;
    private String loginName;
}
