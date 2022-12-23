package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AuthenticateVo {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String userId;      // Identifier of the user within the Casino Operator’s system
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String currency;    // Currency of the player
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal cash;    // Real balance of the player
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal bonus;   // Bonus balance of the player
    private Integer error;      // Response status
    private String description; // Response status short description
}
