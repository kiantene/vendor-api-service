package com.nextgen.gameaggregator.vendor.inout.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.inout.constant.ResponseCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonVo implements HttpResponse {

    private String code;

    private String balance;

    private String message;

    public void setCodeMessages(String responseCode){
        this.code = responseCode;
        this.message = String.valueOf(ResponseCode.valueOf(responseCode));
    }

    @Override
    public boolean hasError() {
        return false;
    }
}
