package com.nextgen.gameaggregator.vendor.esoterica.response;

import com.nextgen.gameaggregator.vendor.esoterica.constant.ResponseCodes;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class ErrorResponse extends CommonResponse {

    public ErrorResponse() {}

    public ErrorResponse(ResponseCodes responseCodes) {
        this.error = responseCodes.getCode();
        this.description = responseCodes.getMessage();
    }
}
