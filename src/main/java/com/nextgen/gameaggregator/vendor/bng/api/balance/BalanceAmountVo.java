package com.nextgen.gameaggregator.vendor.bng.api.balance;

import lombok.Data;

import java.math.BigInteger;

@Data
public class BalanceAmountVo {
    private String value;
    private BigInteger version;
}
