package com.nextgen.gameaggregator.vendor.koolbet.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import lombok.Data;

import java.math.BigDecimal;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {
    private Integer errorCode;
    private String message;
    private String username;
    private String currency;
    private BigDecimal balance;

    public CommonVo() {
        setResponseCode(ResponseCode.SUCCESS);
    }

    public void setResponseCode(ResponseCode responseCode) {
        this.errorCode = responseCode.code;
        this.message = responseCode.message;
    }

    @Override
    public boolean hasError() {
        return this.errorCode != 0;
    }
}
