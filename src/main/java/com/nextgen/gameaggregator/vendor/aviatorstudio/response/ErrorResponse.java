package com.nextgen.gameaggregator.vendor.aviatorstudio.response;

import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ResponseCode;
import lombok.Getter;

@Getter
public class ErrorResponse {
    private final Integer error;
    private final String message;

    public ErrorResponse(ResponseCode responseCode) {
        this.error = responseCode.code;
        this.message = responseCode.description;
    }
}
