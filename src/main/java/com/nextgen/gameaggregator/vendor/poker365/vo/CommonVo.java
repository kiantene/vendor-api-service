package com.nextgen.gameaggregator.vendor.poker365.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.poker365.constant.ResponseCodes;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {


    private String status;
    private BigDecimal balance;
    private String msg;

    public void setResponseCodesandMessage(ResponseCodes responseCode) {
        this.status = String.valueOf(responseCode.getStatus());
        this.msg = responseCode.getMessage();
    }

    @Override
    public boolean hasError() {
        return false;
    }
}
