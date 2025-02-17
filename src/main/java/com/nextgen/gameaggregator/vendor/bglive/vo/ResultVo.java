package com.nextgen.gameaggregator.vendor.bglive.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResultVo {
    private Long userId;
    private String sn;
    private BigDecimal amount;
    private String tranId;
    private String orderResult;

}