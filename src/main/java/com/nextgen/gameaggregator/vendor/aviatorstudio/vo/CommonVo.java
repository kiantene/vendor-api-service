package com.nextgen.gameaggregator.vendor.aviatorstudio.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ResponseCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {
    String id;
    BigDecimal balance;
    String username;
    Integer error;
    String message;

    @JsonIgnore
    private ResponseCode responseCode;

    public CommonVo() {
        this.setResponseCode(ResponseCode.SUCCESS);
    }

    public void setResponseSuccess(BigDecimal balance, String vendorPlayerId, String vendorPlayerUsername) {
        this.id = vendorPlayerId;
        this.balance = balance;
        this.username = vendorPlayerUsername;
        this.responseCode = ResponseCode.SUCCESS;
    }

    public void setResponseCode(ResponseCode responseCode) {
        this.error = responseCode.code;
        this.message = responseCode.description;
    }

    @Override
    public boolean hasError() {
        return this.responseCode != ResponseCode.SUCCESS;
    }
}
