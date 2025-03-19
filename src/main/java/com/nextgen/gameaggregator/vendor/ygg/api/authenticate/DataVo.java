package com.nextgen.gameaggregator.vendor.ygg.api.authenticate;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class DataVo {
    private String playerId;
    private String nickName;
    private String organization;
    private BigDecimal balance;
    private String currency;
    private String homeCurrency;
    private String country;

}
