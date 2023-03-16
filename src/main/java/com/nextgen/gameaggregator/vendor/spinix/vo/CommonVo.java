package com.nextgen.gameaggregator.vendor.spinix.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommonVo {
    private BigDecimal balance;
    private String currency;
}
