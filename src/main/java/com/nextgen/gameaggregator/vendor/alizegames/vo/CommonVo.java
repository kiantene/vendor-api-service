package com.nextgen.gameaggregator.vendor.alizegames.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CommonVo extends ResponseVo {
    private String username;
    private BigDecimal balance;
    private String currency;
    private Long timestamp;
}
