package com.nextgen.gameaggregator.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

@Data
public class OperatorResponseVo<T> {
    private String traceId;
    private String status;
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> validation;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;
}
