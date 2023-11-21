package com.nextgen.gameaggregator.vendor.winfinity.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class DataVo {
    private String transaction;
    private BigDecimal balance;
    private Long timestamp;
}
