package com.nextgen.gameaggregator.vendor.queenmaker.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionsVo extends ResponseVo {

    private String txid;
    private String ptxid;
    private BigDecimal bal;
    private String cur;
}
