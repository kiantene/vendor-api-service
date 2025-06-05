package com.nextgen.gameaggregator.vendor.dblive.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceDataVo {
    private String loginName;
    private BigDecimal balance;
}
