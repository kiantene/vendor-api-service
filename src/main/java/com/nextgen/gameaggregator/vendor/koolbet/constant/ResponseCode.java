package com.nextgen.gameaggregator.vendor.koolbet.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseCode {

    // --- General Codes ---
    SUCCESS(0, "SUCCESS", HttpStatus.OK, false),
    INVALID_PARAMETER(3, "INVALID PARAMETER", HttpStatus.OK, false),
    TOKEN_EXPIRED(4, "TOKEN EXPIRED", HttpStatus.OK, false),
    OTHER_ERROR(5, "OTHER ERROR", HttpStatus.OK, false),

    // --- Bet Specific Codes ---
    BET_ALREADY_ACCEPTED(1, "BET ALREADY ACCEPTED", HttpStatus.OK, false),
    INSUFFICIENT_BALANCE(2, "INSUFFICIENT BALANCE", HttpStatus.OK, false),

    // --- Cancel Specific Codes ---
    ALREADY_CANCELED(1, "ALREADY CANCELED", HttpStatus.OK, false),
    ROUND_NOT_FOUND(2, "ROUND NOT FOUND", HttpStatus.OK, false),
    ALREADY_ACCEPTED_AND_CANNOT_BE_CANCELED(6, "ALREADY ACCEPTED AND CANNOT BE CANCELED", HttpStatus.OK, false),

    // --- Session Cancel Specific ---
    SESSION_CANCEL_BET_SUCCESS(0, "SESSION CANCEL BET SUCCESSFUL", HttpStatus.OK, false),
    SESSION_BET_ALREADY_CANCELED(1, "SESSION BET ALREADY CANCELED", HttpStatus.OK, false),
    SESSION_ROUND_NOT_FOUND(2, "SESSION ROUND NOT FOUND", HttpStatus.OK, false),

    REWARD_SUCCESS(0, "BET SUCCESSFUL", HttpStatus.OK, false),
    REWARD_ALREADY_ACCEPTED(1, "BET ALREADY ACCEPTED", HttpStatus.OK, false),
    REWARD_INVALID_PARAMETER(3, "INVALID PARAMETER", HttpStatus.OK, false),
    REWARD_TOKEN_EXPIRED(4, "TOKEN EXPIRED", HttpStatus.OK, false),
    REWARD_OTHER_ERROR(5, "OTHER ERROR", HttpStatus.OK, false);

    public final Integer code;
    public final String message;
    public final HttpStatus httpStatus;
    private final boolean vendorWillRetry;
}
