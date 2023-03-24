package com.nextgen.gameaggregator.vendor.jdb.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommonVo implements HttpResponse {
    @JsonProperty("status")
    private String status;

    @JsonProperty("balance")
    private BigDecimal balance = BigDecimal.ZERO;

    @JsonProperty("err_text")
    private String errText;

    public void setResponseCode(ResponseCode responseCode) {
        this.setStatus(responseCode.code);
        this.setErrText(responseCode.description);
    }

    @Override
    public boolean hasError() {
        return false;
    }
}
