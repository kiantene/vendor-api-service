package com.nextgen.gameaggregator.vendor.groove.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum OrderType {
    CASH_MONEY("cash_money"),
    BONUS_MONEY("bonus_money");

    public final String value;
}
