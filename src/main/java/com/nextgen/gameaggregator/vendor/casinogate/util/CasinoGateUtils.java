package com.nextgen.gameaggregator.vendor.casinogate.util;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

@UtilityClass
public class CasinoGateUtils {
    private static final int DENOMINATION = 100;

    public static int getDenomination() {
        return DENOMINATION;
    }

    public static BigInteger toMinorUnits(BigDecimal amount) {
        return amount
                .setScale(2, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(DENOMINATION))
                .toBigInteger();
    }

    public static BigDecimal toMajorUnits(long amount) {
        return BigDecimal.valueOf(amount)
                .divide(BigDecimal.valueOf(DENOMINATION));
    }
}
