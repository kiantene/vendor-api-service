package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoundPayoutDataWalletVo {

    private String currency;
    private BigDecimal balance;

}