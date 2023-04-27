package com.nextgen.gameaggregator.vendor.mg.api.login;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class LoginVo {
    private String currency;
    private BigDecimal balance;
    private String extOperatorToken;
}
