package com.nextgen.gameaggregator.vendor.ygg.api.getbalance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DataVo {
    private String currency;
    private BigDecimal applicableBonus;
    private String homeCurrency;
    private String organization;
    private BigDecimal balance;
    private String nickName;
    private String playerId;
    private BigDecimal bonus;
}
