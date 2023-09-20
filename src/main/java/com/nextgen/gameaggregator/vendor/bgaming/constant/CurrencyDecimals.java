package com.nextgen.gameaggregator.vendor.bgaming.constant;

import java.util.HashMap;
import java.util.Map;

public class CurrencyDecimals {
    // TODO: If have new currency code need to update for convert Integer
    public static final String CNY = "CNY";
    public static final String PHP = "PHP";
    // (1 : 1)
    public static final String IDR = "IDR";
    // (1 : 1)
    public static final String VND = "VND";
    public static final String INR = "INR";
    public static final String BRL = "BRL";
    public static final String MYR = "MYR";
    public static final String THB = "THB";
    public static final String MXN = "MXN";

    public static final Map<String, Integer> CURRENCY_DECIMAL = new HashMap<>() {{
        put(CNY, 100);
        put(PHP, 100);
        put(IDR, 1);
        put(VND, 1);
        put(INR, 100);
        put(BRL, 100);
        put(MYR, 100);
        put(THB, 100);
        put(MXN, 100);
    }};
}
