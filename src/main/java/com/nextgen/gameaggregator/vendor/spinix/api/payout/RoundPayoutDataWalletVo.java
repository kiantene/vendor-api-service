package com.nextgen.gameaggregator.vendor.spinix.api.payout;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoundPayoutDataWalletVo {

    private String currency;
    private BigDecimal balance;

}