package com.nextgen.gameaggregator.vendor.spadegaming.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AcctInfoVo {
    private String accId;
    private String userName;
    private String currency;
    private BigDecimal balance;
    private String siteId;
}
