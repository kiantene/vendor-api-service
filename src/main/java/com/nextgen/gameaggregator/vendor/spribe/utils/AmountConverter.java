package com.nextgen.gameaggregator.vendor.spribe.utils;

import java.math.BigDecimal;

import com.nextgen.gameaggregator.vendor.spribe.constant.Unit;

public class AmountConverter {
    public static BigDecimal convertUnitToBalance(BigDecimal unit) {
        return unit.divide(new BigDecimal(Unit.thousand));
    }

    public static BigDecimal convertBalanceToUnit(BigDecimal balance) {
        return balance.multiply(BigDecimal.valueOf(Unit.thousand));
    }
}
