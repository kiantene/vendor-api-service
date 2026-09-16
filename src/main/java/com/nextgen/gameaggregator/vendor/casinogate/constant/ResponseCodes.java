package com.nextgen.gameaggregator.vendor.casinogate.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseCodes {

    INVALID_REQUEST(400, "invalid request payload. ensure all required fields are provided", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(0, "unauthorized", HttpStatus.UNAUTHORIZED),
    INTERNAL_ERROR(0, "something went wrong", HttpStatus.INTERNAL_SERVER_ERROR),
    SESSION_NOT_FOUND(7001, "session validation failed", HttpStatus.NOT_FOUND),
    SESSION_VALIDATION_FAILED(7001, "session validation failed", HttpStatus.BAD_REQUEST),
    ROUND_NOT_FOUND(7004, "game round was not previously created", HttpStatus.BAD_REQUEST),
    TRANSACTION_ALREADY_PROCESSED(7005, "transaction already processed", HttpStatus.BAD_REQUEST),
    BET_TRANSACTION_NOT_FOUND(7006, "bet transaction not found", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_FUNDS(7007, "insufficient funds", HttpStatus.BAD_REQUEST),
    TRANSACTION_ALREADY_REFUNDED(7008, "transaction already refunded", HttpStatus.BAD_REQUEST),
    GAME_ROUND_ALREADY_CLOSED(7019, "game round already closed", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String description;
    private final HttpStatus httpStatus;
}
