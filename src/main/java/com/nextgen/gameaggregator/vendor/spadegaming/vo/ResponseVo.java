package com.nextgen.gameaggregator.vendor.spadegaming.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;

import lombok.Data;

@Data
public class ResponseVo implements HttpResponse{
    private String merchantCode;
    private String msg;
    private Integer code;
    private String serialNo;

    @JsonIgnore
    private ResponseCode responseCode;

    public ResponseVo() {
        this.setResponseCode(ResponseCode.SUCCESS);
    }

    public void setResponseCode(ResponseCode responseCode) {
        this.responseCode = responseCode;
        this.code = responseCode.code;
        this.msg = responseCode.description;
    }

    @Override
    public boolean hasError() {
        return !this.responseCode.equals(ResponseCode.SUCCESS);
    }
}