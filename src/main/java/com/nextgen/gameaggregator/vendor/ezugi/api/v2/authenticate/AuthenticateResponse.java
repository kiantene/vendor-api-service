package com.nextgen.gameaggregator.vendor.ezugi.api.v2.authenticate;

import com.nextgen.gameaggregator.vendor.ezugi.response.SuccessResponse;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class AuthenticateResponse extends SuccessResponse {
    private String token;
    private String uid;
    private BigDecimal balance;
    private String currency;
    private Long timestamp;
}
