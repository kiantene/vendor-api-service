package com.nextgen.gameaggregator.vendor.saba.api.confirmbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmBetTransactionDto {
    private String userId;
    private String refId;
    private Long txId;
    private String licenseeTxId;
    private BigDecimal odds;
    private Integer oddsType;
    private BigDecimal actualAmount;
    private Boolean isOddsChanged;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
    private String winlostDate;
}
