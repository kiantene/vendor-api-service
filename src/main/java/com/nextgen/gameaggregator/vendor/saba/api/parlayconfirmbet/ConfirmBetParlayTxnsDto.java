package com.nextgen.gameaggregator.vendor.saba.api.parlayconfirmbet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConfirmBetParlayTxnsDto {
    private String refId;
    private String txId;
    private String licenseeTxId;
    private BigDecimal actualAmount;
    private Boolean isOddsChanged;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
    private String winlostDate;
    private BigDecimal odds;
}
