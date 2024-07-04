package com.nextgen.gameaggregator.vendor.epicwin.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {
//    // Status Code from Provider
//    OK(200, "OK"),
//    BAD_REQUEST(400, "Bad Request"),
//    FORBIDDEN_ACCESS(403, "Forbidden Access"),
//    SERVICE_MAINTENANCE(503, "Service Maintenance"),
//    INVALID_OPERATOR_ID(900401, "Invalid Operator ID"),
//    INVALID_PLAYER(900402, "Invalid Player"),
//    //    INVALID_SIGNATURE(900403, "Invalid Signature"),
//    INVALID_GAME_ID(900404, "Invalid Game Id"),
//    INVALID_TRAN_ID(900408, "Invalid TranId"),
////    INTERNAL_SERVER_ERROR(900500, "Internal Server Error"),

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
