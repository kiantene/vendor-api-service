package com.nextgen.gameaggregator.vendor.bombay.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.bombay.constant.ResponseCodes;
import lombok.Data;

import java.math.BigInteger;

@Data
public class ResponseVo implements HttpResponse {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String user;

    private String status;

    private String request_uuid;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String currency;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigInteger balance;

    @Override
    public boolean hasError() {

        if(!this.getStatus().equals(ResponseCodes.RS_OK)){
            return true;
        }

        return false;
    }
}
