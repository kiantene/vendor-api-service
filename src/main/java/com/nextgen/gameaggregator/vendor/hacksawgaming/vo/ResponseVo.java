package com.nextgen.gameaggregator.vendor.hacksawgaming.vo;

import com.nextgen.gameaggregator.service.HttpResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import javax.annotation.Nullable;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ResponseVo implements HttpResponse {
    private Integer code;
    private String msg;

    @Nullable
    private ResponseDataVo data;

    @Override
    public boolean hasError() {
        return false;
    }
}