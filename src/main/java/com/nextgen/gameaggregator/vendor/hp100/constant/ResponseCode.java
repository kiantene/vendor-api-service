package com.nextgen.gameaggregator.vendor.hp100.constant;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum ResponseCode {

    SUCCESS(200, "Success", HttpStatus.OK),
    AUTHENTICATION_FAILED(604, "Session not found", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(702, "Internal server error", HttpStatus.BAD_REQUEST),
    INVALID_SECRET_KEY(601, "Invalid secret key", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE(603, "Insufficient balance", HttpStatus.BAD_REQUEST),
    DUPLICATE_REQUEST(607, "Transaction already exists", HttpStatus.BAD_REQUEST),
    DUPLICATE_SETTLE(609, "Transaction already settled", HttpStatus.BAD_REQUEST),
    DUPLICATE_REFUND(608, "Transaction already refunded", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(400, "Validation error", HttpStatus.BAD_REQUEST),


    // @todo Map the vendor's error codes accordingly below.
    BET_NOT_FOUND(605, "Transaction not found", HttpStatus.BAD_REQUEST),
    BET_REJECTED(605, "Bet rejected", HttpStatus.BAD_REQUEST),
    INVALID_PLAYER(604, "Invalid player", HttpStatus.BAD_REQUEST),
    PLAYER_NOT_FOUND(604, "Player not exist", HttpStatus.BAD_REQUEST);

    public final Integer code;
    public final String message;
    public final HttpStatus httpStatus;
}
