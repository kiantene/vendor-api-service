package com.nextgen.gameaggregator.vendor.bng.api.login;

import lombok.Data;

import java.math.BigInteger;

@Data
public class BalanceVo {
    private String value;
    private BigInteger version;
}
