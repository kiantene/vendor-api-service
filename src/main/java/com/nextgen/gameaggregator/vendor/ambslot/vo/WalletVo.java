package com.nextgen.gameaggregator.vendor.ambslot.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletVo {
    private BigDecimal balance;

    private String lastUpdate;
}
