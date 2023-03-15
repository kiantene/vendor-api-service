package com.nextgen.gameaggregator.vendor.spadegaming.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferVo extends ResponseVo {
    private String transferId;
    private String merchantTxId;
    private String acctId;
    private BigDecimal balance;
}
