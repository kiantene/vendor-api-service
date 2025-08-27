package com.nextgen.gameaggregator.vendor.superbullgaming.vo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo extends ResponseVo {
    private String username;
    private BigDecimal balance;
    private String currency;
    private Long timestamp;
    private String token;
    private String operatorId;
}
