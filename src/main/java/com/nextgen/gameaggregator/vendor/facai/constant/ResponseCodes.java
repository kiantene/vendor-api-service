package com.nextgen.gameaggregator.vendor.facai.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {
    SUCCESS             (0, "Success"),
    PLAYER_NOT_FOUND   (500, "Account does not exist."),
    REVERT_CANCEL_BET   (799, "Revert Cancel Bet."),
    TRANSACTION_NOT_EXIST   (221, "Transaction ID number not exist."),
    UNEXPECTED_ERROR     (999, "Unexpected error.")
    ;

    public final Integer code;
    public final String description;
}
