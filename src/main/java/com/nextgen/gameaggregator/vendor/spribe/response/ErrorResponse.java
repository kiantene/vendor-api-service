package com.nextgen.gameaggregator.vendor.spribe.response;

import com.nextgen.gameaggregator.vendor.spribe.constant.ErrorCodes;

public record ErrorResponse(Integer code, String message) {

    public static ErrorResponse of(ErrorCodes errorCodes) {
        return new ErrorResponse(errorCodes.code, errorCodes.description);
    }
}
