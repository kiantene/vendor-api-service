package com.nextgen.gameaggregator.vendor.pgsoft.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ResponseVo<T> implements HttpResponse {

    private T data;
    private CommonErrorVo error;

    public void setError(ResponseCodes responseCode) {
        if (this.error == null) {
            this.error = new CommonErrorVo();
        }
        this.error.setCode(responseCode.getCode());
        this.error.setMessage(responseCode.getMessage());
    }

    @Override
    public boolean hasError() {
        return this.error != null;
    }
}
