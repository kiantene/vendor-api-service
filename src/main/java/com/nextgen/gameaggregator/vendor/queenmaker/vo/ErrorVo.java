package com.nextgen.gameaggregator.vendor.queenmaker.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.ResponseCode;
import lombok.Data;
@Data
public class ErrorVo implements HttpResponse {
    private Integer err;
    private String errdesc;
    @JsonIgnore
    private ResponseCode responseCode;

    public void setResponseCode(ResponseCode responseCode) {
        this.responseCode = responseCode;
        this.err = responseCode.err;
        this.errdesc = responseCode.errdesc;
    }

    @Override
    public boolean hasError() {
        return true;
    }
}
