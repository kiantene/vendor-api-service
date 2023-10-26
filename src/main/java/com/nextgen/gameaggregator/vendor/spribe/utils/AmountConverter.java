package com.nextgen.gameaggregator.vendor.spribe.utils;

import java.math.BigDecimal;

import com.nextgen.gameaggregator.vendor.spribe.constant.Unit;

public class AmountConverter {
    public static BigDecimal convertUnitToBalance(Integer unit) {
        BigDecimal result = new BigDecimal(unit).divide(new BigDecimal(Unit.thousand));
        return result;
    }

    public static BigDecimal convertBalanceToUnit(BigDecimal balance) {
        BigDecimal result = balance.multiply(BigDecimal.valueOf(Unit.thousand));
        return result;
    }
}
