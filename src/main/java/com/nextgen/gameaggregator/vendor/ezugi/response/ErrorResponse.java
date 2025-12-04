package com.nextgen.gameaggregator.vendor.ezugi.response;

import java.math.BigDecimal;

import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import lombok.Data;

@Data
public class ErrorResponse {
    private Integer errorCode;
    private String errorDescription;
    private BigDecimal balance;

    public ErrorResponse(Integer errorCode) {
        this.errorCode = errorCode;
        this.errorDescription = ResponseCodes.RESPONSE_DESCRIPTION.get(errorCode);
        this.balance = BigDecimal.ZERO;
    }

    public ErrorResponse(Integer errorCode, String errorDescription) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.balance = BigDecimal.ZERO;
    }
}
