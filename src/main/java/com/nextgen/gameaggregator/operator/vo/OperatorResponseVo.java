package com.nextgen.gameaggregator.operator.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

import java.util.Map;

@Data
public class OperatorResponseVo<T> implements HttpResponse {
    private String traceId;
    private ResponseCodes.Status status = ResponseCodes.Status.SC_OK;
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> validation;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public void setResponseCode(ResponseCodes.Status responseCodesStatus) {
        this.status = responseCodesStatus;
        this.message = responseCodesStatus.description;
    }

    @Override
    public boolean hasError() {
        return !this.status.equals(ResponseCodes.Status.SC_OK);
    }
}
