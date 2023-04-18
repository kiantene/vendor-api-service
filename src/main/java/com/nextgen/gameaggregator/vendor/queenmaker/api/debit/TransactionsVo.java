package com.nextgen.gameaggregator.vendor.queenmaker.api.debit;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionsVo {

    private String txid;
    private String ptxid;
    private BigDecimal bal;
    private String cur;
    private Boolean dup;
    private Integer err;
    private String errdesc;
}
