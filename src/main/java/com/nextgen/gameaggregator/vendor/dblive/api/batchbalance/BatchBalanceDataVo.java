package com.nextgen.gameaggregator.vendor.dblive.api.batchbalance;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchBalanceDataVo {
    private String loginName;
    private BigDecimal balance;

}
