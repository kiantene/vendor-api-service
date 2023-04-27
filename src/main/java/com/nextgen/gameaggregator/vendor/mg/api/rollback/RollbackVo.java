package com.nextgen.gameaggregator.vendor.mg.api.rollback;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class RollbackVo {
    private String extTxnId;
    private String currency;
    private BigDecimal balance;
    private Long extCreationTimeMs;
}
