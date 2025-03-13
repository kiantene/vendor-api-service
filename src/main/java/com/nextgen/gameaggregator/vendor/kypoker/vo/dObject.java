package com.nextgen.gameaggregator.vendor.kypoker.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class dObject {
    private Integer code;

    private String account;

    private BigDecimal money;

    private Integer roomMode;

    private Integer betCount;

    private BigDecimal totalBet;

    private BigDecimal validBet;

    private BigDecimal totalWithdraw;

    private BigDecimal revenue;

    private Integer status;

}
