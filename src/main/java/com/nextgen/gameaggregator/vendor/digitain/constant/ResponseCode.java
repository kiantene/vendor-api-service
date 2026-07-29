package com.nextgen.gameaggregator.vendor.digitain.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {

    SUCCESS(1, "Success"),
    SESSION_NOT_FOUND_OR_EXPIRED(2, "Session Not Found or Expired"),
    WRONG_PLAYER_ID(4, "Wrong Player Id"),
    PLAYER_IS_BLOCKED(5, "Player Is Blocked"),
    LOW_BALANCE(6, "Low Balance"),
    TRANSACTION_NOT_FOUND(7, "Transaction Not Found"),
    TRANSACTION_ALREADY_EXISTS(8, "Transaction Already Exists"),
    GAME_NOT_FOUND(11, "Game Not Found"),
    WRONG_SECRET_KEY(12, "Wrong Secret Key"),
    PLAYER_BET_LIMIT_EXCEEDED(22, "PlayerBetLimitExceeded"),
    GENERAL_ERROR(999, "GeneralError");

    public final Integer code;
    public final String description;
}
