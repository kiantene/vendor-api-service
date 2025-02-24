package com.nextgen.gameaggregator.vendor.marblex.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.marblex.constant.StatusCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {
    @JsonProperty("TraceID")
    private String traceId;
    @JsonProperty("StatusCode")
    private Integer statusCode;
    @JsonProperty("Data")
    private CommonDataVo data;

    @Override
    public boolean hasError() {
        return !statusCode.equals(StatusCode.SUCCESS);
    }
}
