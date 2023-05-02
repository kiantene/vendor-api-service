package com.nextgen.gameaggregator.vendor.mg.api.login;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginVo {
    private String currency;
    private BigDecimal balance;
    private String extOperatorToken;
}
