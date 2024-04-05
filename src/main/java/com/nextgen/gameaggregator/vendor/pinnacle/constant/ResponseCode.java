package com.nextgen.gameaggregator.vendor.pinnacle.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    SUCCESS(0, "Success"),
    UNKNOWN_ERROR(-1, "Unknown Error"),
    INSUFFICIENT_FUND(-2, "Insufficient Fund"),
    SESSION_NOT_FOUND(-3, "Session Not Found"),
    ACCOUNT_FROZEN(-4, "Account Frozen"),
    ACCOUNT_NOT_FOUND(-5, "Account Not Found"),
    API_AUTHENTICATED_FAILED(-6, "API Authenticated Failed"),
    TRANSACTION_NOT_COMPLETE(-7, "Transaction Not Complete"),
    TRANSACTION_NOT_FOUND(-8, "Transaction Not Found"),
    CURRENCY_MISMATCH(-9, "Currency Mismatch"),
    STAKE_LIMIT(-10, "Stake Limit"),
    WAGER_LIMIT(-11, "Wager Limit"),
    LOSS_LIMIT(-12, "Loss Limit");

    public final Integer code;
    public final String description;
}
