package com.nextgen.gameaggregator.vendor.aviatorstudio.response;

import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ResponseCodes;
import lombok.Getter;

@Getter
public class ErrorResponse {
    private final Integer error;
    private final String message;

    public ErrorResponse(ResponseCodes responseCodes) {
        this.error = responseCodes.getCode();
        this.message = responseCodes.getDescription();
    }

    public static VendorErrorResponse of(ResponseCodes responseCodes) {
        return new VendorErrorResponse(
                responseCodes.getHttpStatus(),
                new ErrorResponse(responseCodes)
        );
    }
}
