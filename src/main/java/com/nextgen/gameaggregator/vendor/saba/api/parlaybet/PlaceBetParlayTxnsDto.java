package com.nextgen.gameaggregator.vendor.saba.api.parlaybet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceBetParlayTxnsDto {
    private String refId;
    private String parlayType;
    private BigDecimal betAmount;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
//    private String details;
}
