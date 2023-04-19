package com.nextgen.gameaggregator.vendor.jdb.vo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;

import lombok.Data;

@Data
public class CommonVo implements HttpResponse {
    @JsonProperty("status")
    private Integer status;

    @JsonProperty("balance")
    private BigDecimal balance = BigDecimal.ZERO;

    @JsonProperty("err_text")
    private String errText;

    public CommonVo() {
        this.setSuccessResponseCode(ResponseCode.SUCCESS);
    }

    public void setSuccessResponseCode(String responseCode) {
        this.status = Integer.valueOf(responseCode);
    }

    public void setResponseCode(String responseCode) {
        this.status = Integer.valueOf(responseCode);
        this.errText = ResponseCode.RESPONSE_DESCRIPTION.get(responseCode);
    }
    @Override
    public boolean hasError() {
        return false;
    }
}
