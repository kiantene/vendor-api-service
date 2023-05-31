package com.nextgen.gameaggregator.vendor.queenmaker.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.ResponseCode;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {

    private Boolean dup;
    private Integer err;
    private String errdesc;

    public void setResponseCode(String errCode) {
        this.setErr(Integer.valueOf(errCode));
        this.setErrdesc(ResponseCode.RESPONSE_DESCRIPTION.get(errCode));
    }

    public void setResponseCode(String errCode, String errDesc) {
        this.setErr(Integer.valueOf(errCode));
        this.setErrdesc(errDesc);
    }

    @Override
    public boolean hasError() {
        return false;
    }
}
