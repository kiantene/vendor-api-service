package com.nextgen.gameaggregator.vendor.kypoker.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResponseObjectDto {
    private Integer code;

    private String account;

    private BigDecimal money;

    private Integer roomMode;

    private Integer status;

}
