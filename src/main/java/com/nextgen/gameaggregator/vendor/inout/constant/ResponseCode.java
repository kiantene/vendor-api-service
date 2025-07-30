package com.nextgen.gameaggregator.vendor.inout.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {

    OK("OK", ""),
    TEMPORARY_ERROR("TEMPORARY_ERROR", "There is a temporary problem with the game server."),
    INVALID_TOKEN("INVALID_TOKEN", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again."),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again."),
    ACCOUNT_INVALID("ACCOUNT_INVALID", "There has been a problem with the casino. User does not exist."),
    UNKNOWN_ERROR("UNKNOWN_ERROR", "Please contact Customer Support for assistance."),
    GAME_DISABLED("GAME_DISABLED", "Game is disabled on the casino side."),
    INSUFFICIENT_FUNDS("INSUFFICIENT_FUNDS", "You do not have sufficient funds to place this bet."),
    CHECKS_FAIL("CHECKS_FAIL", "Operation not allowed because of failed checks on the casino side."),
    DEBIT_TRANSACTION_NOT_FOUND("DEBIT_TRANSACTION_NOT_FOUND", "");

    private final String code;
    private final String message;
}