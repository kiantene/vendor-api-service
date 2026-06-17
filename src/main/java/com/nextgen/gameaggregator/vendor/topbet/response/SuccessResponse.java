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
public class SuccessResponse {
    @Builder.Default
    private final Integer code = ResponseCode.SUCCESS.code;

    @Builder.Default
    private final String message = ResponseCode.SUCCESS.message;

    @JsonProperty("merchant_trans_id")
    private String merchantTransId;

    private BigDecimal balance;
}
