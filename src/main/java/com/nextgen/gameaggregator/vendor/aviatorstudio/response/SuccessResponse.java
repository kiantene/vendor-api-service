package com.nextgen.gameaggregator.vendor.aviatorstudio.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SuccessResponse {
    private String id;
    private BigDecimal balance;
    private String username;
}
