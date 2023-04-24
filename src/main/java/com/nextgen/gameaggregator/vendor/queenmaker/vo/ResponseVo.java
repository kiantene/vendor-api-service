package com.nextgen.gameaggregator.vendor.queenmaker.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.ResponseCode;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {
    private Integer err;
    private String errdesc;

    public void setResponseCode(String errCode) {
        this.err = Integer.valueOf(errCode);
        this.errdesc = ResponseCode.RESPONSE_DESCRIPTION.get(errCode);
    }

    public void setResponseCode(String errCode, String errDesc) {
        this.err = Integer.valueOf(errCode);
        this.errdesc = errDesc;
    }

    @Override
    public boolean hasError() {
        return true;
    }
}
