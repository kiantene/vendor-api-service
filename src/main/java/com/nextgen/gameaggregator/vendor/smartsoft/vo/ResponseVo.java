package com.nextgen.gameaggregator.vendor.smartsoft.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.ResponseCode;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {
    private Integer errorCode;
    private String errorMessage;

    public void setResponseCode(ResponseCode responseCode) {
        this.errorCode = responseCode.code;
        this.errorMessage = responseCode.message;
    }

    @Override
    public boolean hasError() {
        return this.errorCode != 0;
    }
}
