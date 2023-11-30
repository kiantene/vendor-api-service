package com.nextgen.gameaggregator.vendor.saba.api.parlaybet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlaceBetParlayTxnsDto {
    private String refId;
    private String parlayType;
    private BigDecimal betAmount;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
    private String details;
}
