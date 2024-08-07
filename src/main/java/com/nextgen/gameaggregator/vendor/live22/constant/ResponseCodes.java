package com.nextgen.gameaggregator.vendor.live22.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {
    // Status Code from Operator
    OK(200, "OK"),
    INVALID_PLAYER_PASSWORD(900404, "Invalid player / password. Please try again"),
    OPERATOR_ID_ERROR(900405, "Operator ID Error"),
    INCOMING_REQUEST_INFO_INCOMPLETE(900406, "Incoming Request Info Incomplete"),
    INVALID_SIGNATURE(900407, "Invalid Signature"),
    DUPLICATE_TRANSACTION(900409, "Duplicate Transaction"),
    BET_TRANSACTION_NOT_FOUND(900415, "Bet Transaction Not Found"),
    PLAYER_INACTIVE(900416, "Player Inactive"),
    INTERNAL_SERVER_ERROR(900500, "Internal Server Error"),
    INSUFFICIENT_BALANCE(900605, "Insufficient Balance"),
    MAX_PAYOUT_REACHED(800401, "Max Payout Reached");


    public final Integer Status;
    public final String Description;
}
