package com.nextgen.gameaggregator.vendor.cockfight6.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.ResponseCode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FailResponse {
    private Integer code;
    private String message;

    public static FailResponse of(ResponseCode responseCodes) {
        return FailResponse.builder()
                .code(responseCodes.code)
                .message(responseCodes.message)
                .build();
    }
}
