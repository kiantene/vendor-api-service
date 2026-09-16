package com.nextgen.gameaggregator.vendor.casinogate.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigInteger;
import java.util.Map;

@Getter
@Builder
public class CommonResponse {
    private BigInteger balance;
    private Map<String, Object> buffer;
    private String currency;
    private int denomination;
}
