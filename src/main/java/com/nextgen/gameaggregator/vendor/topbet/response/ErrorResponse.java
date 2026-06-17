package com.nextgen.gameaggregator.vendor.topbet.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.topbet.constant.ResponseCode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private Integer code;

    private String message;

    @JsonProperty("merchant_trans_id")
    private String merchantTransId;

    private BigDecimal balance;

    public ErrorResponse(ResponseCode responseCode) {
        this.code = responseCode.code;
        this.message = responseCode.message;
    }

    public ErrorResponse(Integer code, String message, String merchantTransId, BigDecimal balance) {
        this.code = code;
        this.message = message;
        this.merchantTransId = merchantTransId;
        this.balance = balance;
    }

}
