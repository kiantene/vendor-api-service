package com.nextgen.gameaggregator.vendor.vplus.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.vplus.constant.ResponseCodes;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuccessResponse {
    private Integer code;
    private String balance;
    private String message;

    public SuccessResponse() {
        this.code = ResponseCodes.SUCCESS.getCode();
    }

    public static SuccessResponse of(BigDecimal balance) {
        SuccessResponse response = new SuccessResponse();
        response.setBalance(String.valueOf(balance));
        return response;
    }
}
