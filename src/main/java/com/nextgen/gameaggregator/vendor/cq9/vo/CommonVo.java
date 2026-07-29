package com.nextgen.gameaggregator.vendor.cq9.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class CommonVo {
    private BigDecimal balance;
    private String currency;

    public void setBalance(BigDecimal balance) {
        this.balance = (balance == null ? BigDecimal.ZERO : balance)
                .setScale(4, RoundingMode.DOWN);
    }
}
