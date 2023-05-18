package com.nextgen.gameaggregator.vendor.bng.api.bet;

import lombok.Data;

import java.math.BigInteger;

@Data
public class TransactionBalanceVo {
    private String value;
    private BigInteger version;
}
