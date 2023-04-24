package com.nextgen.gameaggregator.vendor.hacksawgaming.vo;

import com.nextgen.gameaggregator.service.HttpResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import javax.annotation.Nullable;

import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ResponseVo implements HttpResponse {
    private Integer code;
    private String msg;

    @Nullable
    private ResponseDataVo data = null;

    @Override
    public boolean hasError() {
        return false;
    }

    public void setCode(Integer responseCode){
        this.code = responseCode;
        this.msg = ResponseCodes.RESPONSE_DESCRIPTION.get(this.code);
    }
}