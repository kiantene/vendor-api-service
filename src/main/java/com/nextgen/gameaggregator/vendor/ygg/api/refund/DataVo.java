package com.nextgen.gameaggregator.vendor.ygg.api.refund;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DataVo {
    private String playerId;
    private String organization;
    private BigDecimal balance;
    private String currency;
}
