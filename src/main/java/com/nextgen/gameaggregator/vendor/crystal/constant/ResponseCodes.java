package com.nextgen.gameaggregator.vendor.crystal.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseCodes {

    INVALID_PARAMETERS      (400, "Request parameters are not valid", HttpStatus.BAD_REQUEST, false),
    INVALID_SIGNATURE       (401, "Signature is not valid", HttpStatus.UNAUTHORIZED, false),
    INSUFFICIENT_FUNDS      (402, "Insufficient funds.", HttpStatus.PAYMENT_REQUIRED, false),
    TXN_NOT_FOUND           (403, "Transaction not found.", HttpStatus.FORBIDDEN, false),
    PLAYER_NOT_FOUND        (404, "Player not found.", HttpStatus.NOT_FOUND, false),
    TXN_ALREADY_SETTLED     (405, "Transaction already settled.", HttpStatus.METHOD_NOT_ALLOWED, false),
    INVALID_ACTION          (406, "The attempted action is invalid.", HttpStatus.NOT_ACCEPTABLE, false)
    ;

    private final Integer code;
    private final String message;
    private final HttpStatus httpStatus;
    private final boolean vendorWillRetry;
}
