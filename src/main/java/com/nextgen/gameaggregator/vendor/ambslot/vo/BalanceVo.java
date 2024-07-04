package com.nextgen.gameaggregator.vendor.ambslot.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceVo {
    private BigDecimal before;

    private BigDecimal after;
}
