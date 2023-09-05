package com.nextgen.gameaggregator.vendor.jili.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.jili.constant.ResponseCode;
import lombok.Data;

@Data
public class ResponseVo implements HttpResponse {
    private Integer errorCode;
    private String message;

    @JsonIgnore
    private ResponseCode responseCode;

    public ResponseVo() {
        this.setResponseCode(ResponseCode.SUCCESS);
    }

    public void setResponseCode(ResponseCode responseCode) {
        this.responseCode = responseCode;
        this.errorCode = responseCode.errorCode;
        this.message = responseCode.message;
    }

    @Override
    public boolean hasError() {
        return !this.responseCode.equals(ResponseCode.SUCCESS);
    }
}
