package com.nextgen.gameaggregator.vendor.koolbet.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import lombok.Data;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenVo implements HttpResponse {
    Integer errorCode;
    String message;
    String username;
    String currency;
    Double balance;

    public TokenVo() {
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
