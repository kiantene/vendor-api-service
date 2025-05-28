package com.nextgen.gameaggregator.vendor.dblive.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {
    SUCCESS(200, "Success"),
    INVALID_PLAYER_SESSION(1001, "Member not exist"),
    INSUFFICIENT_BALANCE(1002, "Insufficient Balance"),
    INVALID_SIGNATURE(8000, "Invalid MD5 Signature"),
    INVALID_PARAMETER(90000, "Invalid Parameter"),
    BET_NOT_FOUND(30007, "Bet not found"),
    DUPLICATE_TRANSACTION(30006, "Duplicate transaction"),
    INVALID_GAME_ID(20001, "Invalid Game ID"),
    OTHER_ERROR(9000, "Other Error"),
    REFUSED_TRANSACTION(1003, "Refuse to process this transaction.");

    public final Integer code;
    public final String description;

}
