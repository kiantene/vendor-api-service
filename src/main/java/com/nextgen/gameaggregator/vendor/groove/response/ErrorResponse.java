package com.nextgen.gameaggregator.vendor.groove.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse extends CommonResponse {
    private String accounttransactionid;
    private String apiversion;
    private BigDecimal balance;
    private BigDecimal bonus_balance;
    private BigDecimal bonusmoneybet;
    private BigDecimal real_balance;
    private BigDecimal realmoneybet;
    private Integer game_mode;
    private String order;

    //BetResult
    private BigDecimal bonusWin;
    private BigDecimal realMoneyWin;
    private String walletTx;
}