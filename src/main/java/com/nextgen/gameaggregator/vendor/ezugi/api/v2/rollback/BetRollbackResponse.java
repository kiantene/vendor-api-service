package com.nextgen.gameaggregator.vendor.ezugi.api.v2.rollback;

import com.nextgen.gameaggregator.vendor.ezugi.response.SuccessResponse;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class BetRollbackResponse extends SuccessResponse {
    private String token;
    private String uid;
    private BigInteger roundId;
    private String transactionId;
    private BigDecimal balance;
    private String currency;
    private Long timestamp;
}
