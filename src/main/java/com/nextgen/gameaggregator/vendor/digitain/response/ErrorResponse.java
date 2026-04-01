package com.nextgen.gameaggregator.vendor.digitain.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.digitain.constant.ResponseCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private Integer err;
    private String txid;
    private String msg;
    private String otxid;
    private String pid;
    private BigDecimal bln;
    private String rid;

    public ErrorResponse(Integer errorCode) {
        this.err = errorCode;
    }

    public ErrorResponse(ResponseCode responseCode) {
        this.err=responseCode.code;
        this.msg =responseCode.description;
    }
}

