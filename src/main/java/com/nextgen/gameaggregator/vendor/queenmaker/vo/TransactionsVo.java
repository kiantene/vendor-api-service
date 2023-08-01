package com.nextgen.gameaggregator.vendor.queenmaker.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Formats;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.ResponseCodes;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionsVo extends ResponseVo {

    private String txid;
    private String ptxid;
    private BigDecimal bal;
    private String cur;

    @Override
    public void setResponseCode(ResponseCodes responseCodes, String errDesc) {
        this.responseCodes = responseCodes;
        setDup(false);
        this.setErr(responseCodes.err);
        this.setErrdesc(responseCodes.errdesc.replace(Formats.REPLACE_STRING, errDesc));
    }

}
