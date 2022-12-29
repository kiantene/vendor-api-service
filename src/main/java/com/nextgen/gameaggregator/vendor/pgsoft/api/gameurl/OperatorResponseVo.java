package com.nextgen.gameaggregator.vendor.pgsoft.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class OperatorResponseVo<T> {
    private String status;
    private String traceId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;
}
