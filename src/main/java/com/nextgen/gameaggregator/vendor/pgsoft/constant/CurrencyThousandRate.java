package com.nextgen.gameaggregator.vendor.pgsoft.constant;

public enum CurrencyThousandRate {
    BIF,
    CDF,
    COP,
    GNF,
    IDR,
    IQD,
    IRR,
    KHR,
    KRW,
    LAK,
    LBP,
    MGA,
    MMK,
    MNT,
    PYG,
    RWF,
    SLL,
    TZS,
    UGX,
    UZS,
    VND;

    public static boolean isThousandRate(String currency) {
        try {
            CurrencyThousandRate.valueOf(currency.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
