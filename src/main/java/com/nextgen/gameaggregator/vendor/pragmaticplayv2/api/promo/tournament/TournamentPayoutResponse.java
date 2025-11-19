package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.promo.tournament;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.constant.ResponseCode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
public class TournamentPayoutResponse {
    private String transactionId;   // Id of the transaction in wallet.
    private String currency;        // Currency of the player
    private BigDecimal cash;        // Real balance of the player
    private BigDecimal bonus;       // Bonus balance of the player

    private Integer error;      // Response status
    private String description; // Response status short description

    @JsonIgnore
    private ResponseCode responseCode;
}
