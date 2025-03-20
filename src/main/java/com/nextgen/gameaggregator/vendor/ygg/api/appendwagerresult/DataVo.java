package com.nextgen.gameaggregator.vendor.ygg.api.appendwagerresult;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DataVo {
    private String playerId;
    private String nickName;
    private String organization;
    private BigDecimal balance;
    private BigDecimal applicableBonus;
    private String currency;
    private String homeCurrency;
}
