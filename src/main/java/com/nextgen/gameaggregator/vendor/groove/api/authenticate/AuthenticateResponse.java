package com.nextgen.gameaggregator.vendor.groove.api.authenticate;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
public class AuthenticateResponse {

    private String accountid;
    private String apiversion;
    private String city;
    private Integer code;
    private String country;
    private String currency;
    private String gamesessionid;
    private BigDecimal real_balance;
    private BigDecimal bonus_balance;
    private String status;
    private Integer game_mode;
    private String order;
}
