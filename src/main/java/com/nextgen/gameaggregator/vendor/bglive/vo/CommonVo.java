package com.nextgen.gameaggregator.vendor.bglive.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonVo implements HttpResponse {
    private String id;
    private String jsonrpc = "2.0";
    private ErrorVo error;
    private Object result;


    public void setErrorResponse(String id, Integer code, String message) {
        this.id = id;
        this.error = new ErrorVo(code, message, message);
    }

    public void setSuccessResponse(String id, Object result) {
        this.id = id;
        this.result = result;
        this.error = null;
    }

    @Override
    public boolean hasError() {
        return false;
    }
}
