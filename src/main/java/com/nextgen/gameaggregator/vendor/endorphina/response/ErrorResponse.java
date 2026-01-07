package com.nextgen.gameaggregator.vendor.endorphina.response;

import com.nextgen.gameaggregator.vendor.endorphina.constant.ResponseCodes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {
    private final String code;
    private final String message;

    public ErrorResponse(ResponseCodes responseCodes) {
        this.code = responseCodes.getCode();
        this.message = responseCodes.getMessage();
    }
}
