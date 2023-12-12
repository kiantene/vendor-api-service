package com.nextgen.gameaggregator.vendor.saba.api.cancelbet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CancelBetTxnsDto {
    private String refId;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
}
