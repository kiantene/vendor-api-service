package com.nextgen.gameaggregator.vendor.groove.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.groove.constant.ResponseCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse {
    private Integer code;
    private String status;
    private String message;

    public CommonResponse() {
        // Default constructor
    }

    public void setError(ResponseCode responseCode) {
        this.code = responseCode.code;
        this.status = responseCode.message;
        this.message = responseCode.message;
    }
}
