package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AuthenticateResponse {
    private String id;
    private BigDecimal balance;
    private String username;
}
