package com.nextgen.gameaggregator.vendor.vplus.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.vplus.constant.ResponseCodes;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private Integer code;
    private String balance;
    private String message;

    public static ErrorResponse of(ResponseCodes responseCode) {
        ErrorResponse err = new ErrorResponse();
        err.setCode(responseCode.getCode());
        err.setMessage(responseCode.getMessage());
        err.setBalance(String.valueOf(BigDecimal.ZERO));
        return err;
    }
}
