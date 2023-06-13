package com.nextgen.gameaggregator.vendor.alizegames.api.balance;

import java.math.BigDecimal;

import com.nextgen.gameaggregator.vendor.alizegames.vo.ResponseVo;

import lombok.Data;

@Data
public class BalanceVo extends ResponseVo {
    private String username;
    private BigDecimal balance;
    private String currency;
}
