package com.nextgen.gameaggregator.vendor.queenmaker.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.ResponseCode;
import lombok.Data;
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {
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
