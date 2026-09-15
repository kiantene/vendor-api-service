package com.nextgen.gameaggregator.vendor.esoterica.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseCodes {

    SUCCESS                 (0, "Success", HttpStatus.OK, true),
    INSUFFICIENT_FUNDS      (1, "Insufficient balance", HttpStatus.PAYMENT_REQUIRED, true),
    PLAYER_NOT_FOUND        (2, "Player not found or is logged out", HttpStatus.FORBIDDEN, true),
    BET_NOT_ALLOWED         (3, "Bet is not allowed", HttpStatus.INTERNAL_SERVER_ERROR, true),
    AUTHENTICATION_FAILED   (4, "Player authentication failed due to invalid, not found or expired token", HttpStatus.INTERNAL_SERVER_ERROR, true),
    TOKEN_NOT_FOUND         (5, "Invalid hash code", HttpStatus.INTERNAL_SERVER_ERROR, true),
    INVALID_PARAMETER       (7, "Bad parameters in the request", HttpStatus.INTERNAL_SERVER_ERROR, true),
    INTERNAL_ERROR          (10, "Internal Server error. Operator logic requires a retry", HttpStatus.INTERNAL_SERVER_ERROR, true),
    INTERNAL_ERROR_NO_RETRY (11, "Internal Server error. Operator logic doesnʼt require a retry", HttpStatus.INTERNAL_SERVER_ERROR, false),
    DUPLICATE_REQUEST       (13, "Operation was successfully completed on the previous request (in case retry)", HttpStatus.INTERNAL_SERVER_ERROR, false),
    ;

    private final Integer code;
    private final String message;
    private final HttpStatus httpStatus;
    private final boolean vendorWillRetry;
}
