package com.nextgen.gameaggregator.vendor.mtlive.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.mtlive.constant.ResponseCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ErrorResponse {

    private String code;

    private String message;

    private long timestamp;

    private Object data = null;

    public ErrorResponse(ResponseCode responseCode) {
        this.code = responseCode.code;
        this.message = responseCode.message;
        this.timestamp = Instant.now().getEpochSecond();
    }
}

