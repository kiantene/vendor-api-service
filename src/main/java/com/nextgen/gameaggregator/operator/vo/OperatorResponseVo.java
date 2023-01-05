package com.nextgen.gameaggregator.operator.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import lombok.Data;

import java.util.Map;

@Data
public class OperatorResponseVo<T> {
    private String traceId;
    private ResponseCodes.Status status = ResponseCodes.Status.SC_OK;
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> validation;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    @JsonIgnore
    public boolean isError() {
        return !this.status.equals(ResponseCodes.Status.SC_OK);
    }
}
