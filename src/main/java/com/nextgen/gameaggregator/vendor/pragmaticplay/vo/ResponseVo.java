package com.nextgen.gameaggregator.vendor.pragmaticplay.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.ResponseCode;
import lombok.Data;

@Data
public class ResponseVo implements HttpResponse {
    private Integer error;      // Response status
    private String description; // Response status short description

    @JsonIgnore
    private ResponseCode responseCode;

    public ResponseVo() {
        this.setResponseCode(ResponseCode.SUCCESS);
    }

    public void setResponseCode(ResponseCode responseCode) {
        this.responseCode = responseCode;
        this.error = responseCode.code;
        this.description = responseCode.description;
    }

    @Override
    public boolean hasError() {
        return !this.responseCode.equals(ResponseCode.SUCCESS);
    }
}
