package com.nextgen.gameaggregator.vendor.spinix.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceDataWalletVo {

    private String currency;
    private BigDecimal balance;

}