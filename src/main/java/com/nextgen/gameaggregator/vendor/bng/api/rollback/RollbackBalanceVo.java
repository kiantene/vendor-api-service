package com.nextgen.gameaggregator.vendor.bng.api.rollback;

import lombok.Data;

import java.math.BigInteger;

@Data
public class RollbackBalanceVo {
    private String value;
    private BigInteger version;
}
