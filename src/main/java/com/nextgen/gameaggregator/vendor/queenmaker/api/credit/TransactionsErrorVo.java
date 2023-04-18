package com.nextgen.gameaggregator.vendor.queenmaker.api.credit;

import lombok.Data;

@Data
public class TransactionsErrorVo implements Transactions {

    private String txid;
    private String ptxid;
    private Integer err;
    private String errdesc;
}
