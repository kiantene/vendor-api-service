package com.nextgen.gameaggregator.vendor.groove.api.betandresult;

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
public class BetAndResultResponse extends CommonResponse {
    private String apiversion;
    private BigDecimal balance;
    private BigDecimal bonus_balance;
    private BigDecimal bonusmoneybet;
    private BigDecimal bonusWin;
    private BigDecimal real_balance;
    private BigDecimal realmoneybet;
    private BigDecimal realMoneyWin;
    private String walletTx;
    private Integer game_mode;
    private String order;
}
