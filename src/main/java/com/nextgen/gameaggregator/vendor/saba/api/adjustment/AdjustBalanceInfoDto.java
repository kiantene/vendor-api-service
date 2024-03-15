package com.nextgen.gameaggregator.vendor.saba.api.adjustment;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdjustBalanceInfoDto {
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
}
