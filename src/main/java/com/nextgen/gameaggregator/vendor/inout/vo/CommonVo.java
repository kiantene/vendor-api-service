package com.nextgen.gameaggregator.vendor.inout.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.inout.constant.ResponseCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {
    private String code;

    private String userId;

    private String nickname;

    private String balance;

    private String currency;

    private String operator;

    private String message;

    public CommonVo() {
        this.code = ResponseCode.OK.code;
        this.message = ResponseCode.OK.message;
    }

    public void setError(ResponseCode responseCode) {
        this.code = responseCode.code;
        this.message = responseCode.message;
    }

    @Override
    public boolean hasError() {
        return !"OK".equalsIgnoreCase(this.code);
    }
}
