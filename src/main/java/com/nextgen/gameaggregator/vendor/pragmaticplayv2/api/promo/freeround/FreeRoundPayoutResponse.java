package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.promo.freeround;

import com.nextgen.gameaggregator.vendor.pragmaticplayv2.constant.ResponseCode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
public class FreeRoundPayoutResponse {
    private String transactionId;   // Id of the transaction in wallet.
    private String currency;        // Currency of the player
    private BigDecimal cash;        // Real balance of the player
    private BigDecimal bonus;       // Bonus balance of the player

    @Builder.Default
    private Integer error = ResponseCode.SUCCESS.code;      // Response status
    @Builder.Default
    private String description = ResponseCode.SUCCESS.description; // Response status short description
}
