package com.nextgen.gameaggregator.vendor.ambslot.vo;

import lombok.Data;

@Data
public class DataVo {
    private String username;

    private WalletVo wallet;

    private BalanceVo balance;

    private String refId;
}
