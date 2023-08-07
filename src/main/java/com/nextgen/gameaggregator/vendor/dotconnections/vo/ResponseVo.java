package com.nextgen.gameaggregator.vendor.dotconnections.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.ResponseCodes;
import lombok.Data;

import javax.annotation.Nullable;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ResponseVo implements HttpResponse {
    private String code;
    private String msg;

    @Nullable
    private ResponseDataVo data = null;

    @Override
    public boolean hasError() {
        return this.code == ResponseCodes.SUCCESS;
    }

    public void setCode(String responseCode) {
        this.code = responseCode;
        this.msg = ResponseCodes.RESPONSE_DESCRIPTION.get(this.code);
    }
}