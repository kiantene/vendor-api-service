package com.nextgen.gameaggregator.vendor.cosmoplay.response;

import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;

public record ErrorResponse(Integer error, String message) {
    public static VendorErrorResponse of(ResponseCode responseCode, String error) {
        String message = error != null && !error.isEmpty()
                ? error
                : responseCode.getMessage();

        ErrorResponse body = new ErrorResponse(
                responseCode.getCode(), message
        );

        return new VendorErrorResponse(responseCode.getStatus(), body);
    }
}
