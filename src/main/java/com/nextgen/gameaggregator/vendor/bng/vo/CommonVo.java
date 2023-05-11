package com.nextgen.gameaggregator.vendor.bng.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommonVo {
    private BigDecimal balance;
    private String currency;
}
