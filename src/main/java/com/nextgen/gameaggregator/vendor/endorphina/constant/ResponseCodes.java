package com.nextgen.gameaggregator.vendor.endorphina.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseCodes {

    ACCESS_DENIED           ("ACCESS_DENIED", "The authentication credentials for the API are incorrect", HttpStatus.UNAUTHORIZED, false),
    INSUFFICIENT_FUNDS      ("INSUFFICIENT_FUNDS", "Player has insufficient funds.", HttpStatus.PAYMENT_REQUIRED, false),
    TOKEN_EXPIRED           ("TOKEN_EXPIRED", "The session token expired.", HttpStatus.FORBIDDEN, false),
    LIMIT_REACHED           ("LIMIT_REACHED", "Limit reached.", HttpStatus.FORBIDDEN, false),
    TOKEN_NOT_FOUND         ("TOKEN_NOT_FOUND", "The session token is invalid.", HttpStatus.NOT_FOUND, false),
    NO_RETRY                ("NO_RETRY", "No retry.", HttpStatus.TOO_MANY_REQUESTS, false),
    INTERNAL_ERROR          ("INTERNAL_ERROR", "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR, false)
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    private final boolean vendorWillRetry;
}