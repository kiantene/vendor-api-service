package com.nextgen.gameaggregator.vendor.evolutionlive.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    OK("OK", "OK"),
    TEMPORARY_ERROR("TEMPORARY_ERROR", "There is a temporary problem with the game serverError Code: 1001"),
    INVALID_TOKEN_ID("INVALID_TOKEN_ID", "Your session has expired. Please log in againError Code: 10003"),
    INVALID_SID("INVALID_SID", "Your session has expired. Please log in againError Code: 10003"),
    UNKNOWN_ERROR("UNKNOWN_ERROR", "Please contact Customer Support for assistanceError Code: 1049"),
    INVALID_PARAMETER("INVALID_PARAMETER", "Please contact Customer Support for assistanceError Code: 10002"),
    BET_DOES_NOT_EXIST("BET_DOES_NOT_EXIST", "Please contact Customer Support for assistanceError Code: 10005"),
    INSUFFICIENT_FUNDS("INSUFFICIENT_FUNDS", "You do not have sufficient funds to place this betError Code: 10008");

    public final String status;
    public final String errorMessage;
}
