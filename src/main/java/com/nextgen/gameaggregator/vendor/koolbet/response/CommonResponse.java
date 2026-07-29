package com.nextgen.gameaggregator.vendor.koolbet.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse {
    private Integer errorCode;
    private String message;
    private String username;
    private String currency;
    private BigDecimal balance;

    public void setResponseCode(ResponseCode responseCode) {
        this.errorCode = responseCode.code;
        this.message = responseCode.message;
    }

}
