package com.nextgen.gameaggregator.vendor.lucky365.response;

import com.nextgen.gameaggregator.vendor.lucky365.constant.ResponseCodes;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private String code;
    private Object data;

    public static ErrorResponse of(ResponseCodes responseCodes) {
        return ErrorResponse.builder()
                .code(responseCodes.getCode())
                .data(null)
                .build();
    }
}
