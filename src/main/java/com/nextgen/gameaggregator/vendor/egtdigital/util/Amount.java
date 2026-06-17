package com.nextgen.gameaggregator.vendor.egtdigital.util;


import java.math.BigDecimal;
import java.math.RoundingMode;


public class Amount {

    private static final BigDecimal TO_VENDOR = BigDecimal.valueOf(100);

    /**
     * Vendor → Internal
     * Example:
     *   Request amount = 50
     *   Internal amount = 0.50
     */
    public static BigDecimal internal(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        return value.divide(TO_VENDOR, 8, RoundingMode.DOWN);
    }

    /**
     * Internal → Vendor
     * Example:
     *   Internal amount = 0.50
     *   Response amount = 50
     */
    public static Long vendor(BigDecimal value) {
        if (value == null) {
            return 0L;
        }

        return value
                .multiply(TO_VENDOR)
                .setScale(0, RoundingMode.DOWN)
                .longValueExact();
    }

    /**
     * Safe integer conversion
     */
    public static Integer integer(Object value) {
        if (value == null) return null;

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