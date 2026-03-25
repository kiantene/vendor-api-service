package com.nextgen.gameaggregator.vendor.cosmoplay.response;

import com.nextgen.gameaggregator.core.exception.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseCode {
    CANCELED (1,HttpStatus.REQUEST_TIMEOUT, "", false),
    UNKNOWN (2, HttpStatus.INTERNAL_SERVER_ERROR, "", false),
    INVALID_ARGUMENT (3, HttpStatus.BAD_REQUEST, "", false),
    DEADLINE_EXCEEDED (4, HttpStatus.GATEWAY_TIMEOUT, "", false),
    NOT_FOUND (5, HttpStatus.NOT_FOUND, "", false),
    ALREADY_EXISTS (6, HttpStatus.CONFLICT, "", false),
    PERMISSION_DENIED (7, HttpStatus.FORBIDDEN, "", false),
    RESOURCE_EXHAUSTED (8, HttpStatus.TOO_MANY_REQUESTS, "", false),
    FAILED_PRECONDITION (9, HttpStatus.BAD_REQUEST, "", false),
    ABORTED (10, HttpStatus.CONFLICT, "", false),
    OUT_OF_RANGE (11, HttpStatus.BAD_REQUEST, "", false),
    UNIMPLEMENTED (12, HttpStatus.NOT_IMPLEMENTED, "", false),
    INTERNAL (13, HttpStatus.INTERNAL_SERVER_ERROR, "", false),
    UNAVAILABLE (14, HttpStatus.SERVICE_UNAVAILABLE, "", false),
    DATA_LOSS (15, HttpStatus.INTERNAL_SERVER_ERROR, "", false),
    UNAUTHORIZED(16, HttpStatus.UNAUTHORIZED, "", false),
    BALANCE_INSUFFICIENT (17, HttpStatus.OK, "Not enough balance", false),
    ;

    private final Integer code;       // The vendor error code.
    private final HttpStatus status;
    private final String message;
    private final Boolean canRetry;

    public static ResponseCode rollback(RollbackNotAllowedException exception) {
        if (exception.isRoundAlreadyEnded()) {
            return ResponseCode.DEADLINE_EXCEEDED;
        }

        if (exception.isBetNotFound()) {
            return ResponseCode.NOT_FOUND;
        }

        if (exception.isBetAlreadySettled()) {
            return ResponseCode.ABORTED;
        }

        return UNAUTHORIZED;
    }

    public static ResponseCode betNotAllowed(BetNotAllowedException exception) {
        if (exception.isRoundAlreadyEnded()) {
            return ResponseCode.DEADLINE_EXCEEDED;
        }

        if (exception.isMultipleBetNotAllowed()) {
            return ResponseCode.ALREADY_EXISTS;
        }

        if (exception.isAllowMultipleBet()) {
            return ResponseCode.FAILED_PRECONDITION;
        }

        return PERMISSION_DENIED;
    }

    public static ResponseCode betResultRejected(BetResultRejectedException exception) {
        if (exception.isRoundAlreadyEnded()) {
            return ResponseCode.DEADLINE_EXCEEDED;
        }

        if (exception.isRoundAlreadyRefunded()) {
            return ResponseCode.PERMISSION_DENIED;
        }

        if (exception.isBetNotFound()) {
            return ResponseCode.NOT_FOUND;
        }

        if (exception.isRoundNotFound()) {
            return ResponseCode.NOT_FOUND;
        }

        if (exception.isRoundAlreadyRefunded()) {
            return ResponseCode.PERMISSION_DENIED;
        }

        if (exception.isBetAndResult()) {
            return ResponseCode.INTERNAL;
        }

        return PERMISSION_DENIED;
    }
}
