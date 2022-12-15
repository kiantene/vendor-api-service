package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.authenticate;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AuthenticateVo {
    private String userId;      // Identifier of the user within the Casino Operator’s system
    private String currency;    // Currency of the player
    private BigDecimal cash;    // Real balance of the player
    private BigDecimal bonus;   // Bonus balance of the player
    private Integer error;      // Response status
    private String description; // Response status short description
}
