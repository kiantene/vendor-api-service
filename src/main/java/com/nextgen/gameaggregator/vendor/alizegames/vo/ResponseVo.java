package com.nextgen.gameaggregator.vendor.alizegames.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.alizegames.constant.ResponseCode;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo<T> implements HttpResponse {
    private Integer code;
    private String status;
    private T data;

    @JsonIgnore
    private ResponseCode responseCode;

    public ResponseVo() {
        this.setResponseCode(ResponseCode.ERROR);
    }

    public void setResponseCode(ResponseCode responseCode) {
        this.responseCode = responseCode;
        this.code = responseCode.code;
        this.status = responseCode.description;
    }

    @Override
    public boolean hasError() {
        return !this.responseCode.equals(ResponseCode.ERROR);
    }
}
