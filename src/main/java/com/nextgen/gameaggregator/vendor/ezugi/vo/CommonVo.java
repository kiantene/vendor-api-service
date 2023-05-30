package com.nextgen.gameaggregator.vendor.ezugi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {
    private Integer operatorId;
    private String token;
    private Integer errorCode;
    private String errorDescription;
    private Long timestamp;

    @Override
    public boolean hasError() {
        return false;
    }
}
