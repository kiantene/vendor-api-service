package com.nextgen.gameaggregator.vendor.hp100.util;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
public class StrictBigDecimalConverter {
    public BigDecimal getAmountAsBigDecimal(String amount) {
        //todo convert amount to 2 decimal place 0.019 should become 0.01

        if (amount != null) {
            BigDecimal decimalAmount = new BigDecimal(amount); // ✅ string constructor
            BigDecimal result = decimalAmount.setScale(2, RoundingMode.DOWN);//cannot directly convert 
            return result;
        } else
            return null;
    }
}
