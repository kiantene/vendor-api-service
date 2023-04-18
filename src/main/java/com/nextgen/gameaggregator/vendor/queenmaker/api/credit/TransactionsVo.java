package com.nextgen.gameaggregator.vendor.queenmaker.api.credit;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionsVo implements Transactions {

    private String txid;
    private String ptxid;
    private BigDecimal bal;
    private String cur;
    private Boolean dup;
}
