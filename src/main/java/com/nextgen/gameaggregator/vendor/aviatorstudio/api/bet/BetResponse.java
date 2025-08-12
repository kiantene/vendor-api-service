package com.nextgen.gameaggregator.vendor.aviatorstudio.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BetResponse {
    private String id;
    private BigDecimal balance;
    private String username;
    private Integer error;
    private String message;
}
