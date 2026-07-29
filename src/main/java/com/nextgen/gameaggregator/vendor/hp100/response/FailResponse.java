package com.nextgen.gameaggregator.vendor.hp100.response;

import com.nextgen.gameaggregator.vendor.hp100.constant.ResponseCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FailResponse {
    private String data;
    private Integer errCode;
    private String message;

    public FailResponse(ResponseCode responseCode) {
        this.data = "";
        this.errCode = responseCode.code;
        this.message = responseCode.message;
    }

    public FailResponse() {

    }
}
