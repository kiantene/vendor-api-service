package com.nextgen.gameaggregator.vendor.queenmaker.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Formats;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.ResponseCode;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {

    private Boolean dup;
    private Integer err;
    private String errdesc;

    public void setResponseCode(ResponseCode responseCode) {
        this.setErr(responseCode.err);
        this.setErrdesc(responseCode.errdesc.replace(Formats.REPLACE_STRING, ""));
    }

    public void setResponseCode(ResponseCode responseCode, String errDesc) {
        this.setErr(responseCode.err);
        this.setErrdesc(errDesc);
    }

    @Override
    public boolean hasError() {
        return false;
    }
}
