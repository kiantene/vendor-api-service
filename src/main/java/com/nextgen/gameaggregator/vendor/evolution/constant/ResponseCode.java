package com.nextgen.gameaggregator.vendor.evolution.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    OK("OK", "OK"),
    TEMPORARY_ERROR("TEMPORARY_ERROR", "There is a temporary problem with the game serverError Code: 1001"),
    INVALID_TOKEN_ID("INVALID_TOKEN_ID", "Your session has expired. Please log in againError Code: 10003"),
    INVALID_SID("INVALID_SID", "Your session has expired. Please log in againError Code: 10003"),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "Your account has been suspended. Please contact Customer Support for assistanceError Code: 10007"),
    UNKNOWN_ERROR("UNKNOWN_ERROR", "Please contact Customer Support for assistanceError Code: 1049"),
    INVALID_PARAMETER("INVALID_PARAMETER", "Please contact Customer Support for assistanceError Code: 10002"),
    BET_DOES_NOT_EXIST("BET_DOES_NOT_EXIST", "Please contact Customer Support for assistanceError Code: 10005"),
    BET_ALREADY_EXIST("BET_ALREADY_EXIST", "Bet already exists in third party system."),
    BET_ALREADY_SETTLED("BET_ALREADY_SETTLED", "Bet already settled in third party system."),
    INSUFFICIENT_FUNDS("INSUFFICIENT_FUNDS", "You do not have sufficient funds to place this betError Code: 10008"),
    FINAL_ERROR_ACTION_FAILED("FINAL_ERROR_ACTION_FAILED", "The attempted action failed. Please try againError Code: 2001");

    public final String status;
    public final String errorMessage;
}
