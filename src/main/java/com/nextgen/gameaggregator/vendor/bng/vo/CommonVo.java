package com.nextgen.gameaggregator.vendor.bng.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {
    @JsonProperty("name")
    private String name;

    @JsonProperty("error")
    private Object error;

    @Override
    public boolean hasError() {
        return false;
    }
}
