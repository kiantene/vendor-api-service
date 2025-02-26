package com.nextgen.gameaggregator.vendor.wmlive.api.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.wmlive.constant.ResponseCode;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseVo implements HttpResponse {

    private DataVo result;
    private Integer errorCode;
    private String errorMessage;

    public ResponseVo() {
        setResponseCodeMsg(ResponseCode.SUCCESS);
    }

    public void setResponseCodeMsg(ResponseCode responseCode) {
        this.errorCode = responseCode.code;
        this.errorMessage = responseCode.message;
    }

    @Override
    public boolean hasError() {
        return this.errorCode != 0;
    }
}

