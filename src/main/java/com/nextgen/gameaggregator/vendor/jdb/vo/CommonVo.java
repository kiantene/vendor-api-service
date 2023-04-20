package com.nextgen.gameaggregator.vendor.jdb.vo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {
    @JsonProperty("status")
    private String status;

    @JsonProperty("balance")
    private BigDecimal balance = BigDecimal.ZERO;

    @JsonProperty("err_text")
    private String err_text;

    public CommonVo() {
        this.setSuccessResponseCode(ResponseCode.SUCCESS);
    }

    public void setSuccessResponseCode(String responseCode) {
        this.status = responseCode;
    }

    public void setErrorResponseCode(String responseCode) {
        this.status = responseCode;
        this.err_text = ResponseCode.RESPONSE_DESCRIPTION.get(responseCode);
    }
    @Override
    public boolean hasError() {
        return false;
    }
}
