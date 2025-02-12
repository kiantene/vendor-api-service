package com.nextgen.gameaggregator.vendor.bglive.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {

    SYSTEM_ERROR(000, "System Error"),
    MISSING_PARAMETERS(700, "Missing Parameters"),
    ERROR_MAX_LENGTH(707, "The parameter maximum length is incorrect"),
    AUTH_INVALID(2203, "Signature of Auth TokenInvalid"),
    PLAYER_INVALID(2405, "This user account does not exist"),
    INSUFFICIENT_BALANCE(2504, "Insufficient Balance"),
    TICKET_INVALID(2511, "Ticket does not exist");

    public final Integer code;
    public final String message;
}
