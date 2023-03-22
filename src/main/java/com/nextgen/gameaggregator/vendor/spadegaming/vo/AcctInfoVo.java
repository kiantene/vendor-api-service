package com.nextgen.gameaggregator.vendor.spadegaming.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class AcctInfoVo {
    private String acctId;
    private String userName;
    private String currency;
    private BigDecimal balance;
    private Integer siteId;
}
