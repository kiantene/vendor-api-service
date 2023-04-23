package com.nextgen.gameaggregator.vendor.queenmaker.api.credit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.ResponseCode;
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

    public void setResponseCode(String errCode) {
        this.err = Integer.valueOf(errCode);
        this.errdesc = ResponseCode.RESPONSE_DESCRIPTION.get(errCode);
    }

    public void setResponseCode(String errCode, String errDesc) {
        this.err = Integer.valueOf(errCode);
        this.errdesc = errDesc;
    }
}
