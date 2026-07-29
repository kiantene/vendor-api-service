package com.nextgen.gameaggregator.vendor.cosmoplay.util;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

@UtilityClass
public class Amount {
    public static final int DEFAULT_DECIMAL_PLACE = 2;
    public static final BigDecimal SCALE = BigDecimal.valueOf(100);

    //Convert to internal use e.g vendor send in 1000 to 10.00
    //For details about converting see : https://new-future.atlassian.net/browse/OVI-997
    public static BigDecimal internal(BigInteger value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(value).divide(
                SCALE,
                DEFAULT_DECIMAL_PLACE,
                RoundingMode.DOWN
        );
    }


    //Convert to vendor format e.g convert send in 10.00 to 1000
    public static Long vendor(BigDecimal value) {
        if (value == null) {
            return 0L;
        }

        return value.multiply(SCALE).setScale(0, RoundingMode.DOWN).longValue();
    }

    public static Integer integer(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
