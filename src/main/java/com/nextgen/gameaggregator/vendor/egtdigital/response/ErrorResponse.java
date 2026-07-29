package com.nextgen.gameaggregator.vendor.egtdigital.response;

import com.nextgen.gameaggregator.vendor.egtdigital.constant.ResponseCodes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {

    private String statusCode;

    public ErrorResponse(ResponseCodes responseCodes) {

        this.statusCode = responseCodes.getCode();
    }
}
