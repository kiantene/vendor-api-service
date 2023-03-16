package com.nextgen.gameaggregator.vendor.facai.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {
    SUCCESS                 (0, "Success"),
    INSUFFICIENT_BALANCE    (203, "Your points balance not enough."),
    GAME_NOT_FOUND          (405, "Game does not exist."),
    PLAYER_NOT_FOUND        (500, "Account does not exist."),
    REVERT_CANCEL_BET       (799, "Revert Cancel Bet."),
    TRANSACTION_NOT_EXIST   (221, "Transaction ID number not exist."),
    UNEXPECTED_ERROR        (999, "Unexpected error."),
    CURRENCY_MISSING        (1012, "Currency code is missing."),
    DATE_INPUT_MISSING       (1018, "Date input is missing."),
    GAME_TYPE_MISSING       (1019, "Game type is missing."),
    PARAM_CONTAIN_ERROR     (1099, "The parameter contain error, please check the parameter is correct or not.")
    ;

    public final Integer code;
    public final String description;
}
