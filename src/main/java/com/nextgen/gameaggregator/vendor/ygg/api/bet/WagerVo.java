package com.nextgen.gameaggregator.vendor.ygg.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.ygg.constant.ResponseCode;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WagerVo implements HttpResponse {
    private int code;
    private DataVo data;
    private String message;

    @JsonIgnore
    private ResponseCode responseCode;

    public void setResponseCode(ResponseCode responseCode) {
        this.responseCode = responseCode;
        this.code = responseCode.code;
        this.message = responseCode.message;
    }

    @Override
    public boolean hasError() {
        return this.code != (0);
    }
}
