package com.nextgen.gameaggregator.vendor.alizegames.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.alizegames.constant.ResponseCode;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {
    private Integer error;
    private String message;

    @JsonIgnore
    private ResponseCode responseCode;

    public ResponseVo() {
        this.setResponseCode(ResponseCode.ERROR);
    }

    public void setResponseCode(ResponseCode responseCode) {
        this.responseCode = responseCode;
        this.error = responseCode.code;
        this.message = responseCode.description;
    }

    @Override
    public boolean hasError() {
        return !this.responseCode.equals(ResponseCode.ERROR);
    }
}
