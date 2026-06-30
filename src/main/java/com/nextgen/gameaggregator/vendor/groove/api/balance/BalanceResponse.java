package com.nextgen.gameaggregator.vendor.groove.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.groove.response.CommonResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceResponse extends CommonResponse {
    private String apiversion;
    private BigDecimal balance;
    private BigDecimal bonus_balance;
    private BigDecimal real_balance;
    private Integer game_mode;
    private String order;
}
