package com.nextgen.gameaggregator.vendor.jili.api.freespin;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@Builder
public class JiliFreeSpinPayoutRequest {
    private String vendorPlayerUsername;
    private String vendorCurrencyCode;
    private String token;
    private String reqId;
    private String round;
    private BigDecimal winloseAmount;
    private BigInteger wagersTime;
    private FreeSpinData freeSpinData;
}
