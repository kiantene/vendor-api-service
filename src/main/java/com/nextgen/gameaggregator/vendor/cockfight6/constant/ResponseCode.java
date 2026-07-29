package com.nextgen.gameaggregator.vendor.cockfight6.constant;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum ResponseCode {

    SUCCESS(1, "Success", HttpStatus.OK),
    SESSION_NOT_FOUND(304, "Session not found", HttpStatus.OK),
    INTERNAL_ERROR(500, "Internal server error", HttpStatus.OK),
    INVALID_SECRET_KEY(302, "Invalid secret key", HttpStatus.OK),
    INSUFFICIENT_BALANCE(301, "Insufficient balance", HttpStatus.OK),
    DUPLICATE_REQUEST(307, "Transaction already exists", HttpStatus.OK),
    DUPLICATE_REFUND(308, "Transaction already refunded", HttpStatus.OK),
    INVALID_REQUEST(400, "Invalid Request", HttpStatus.OK),


    // @todo Map the vendor's error codes accordingly below.
    BET_NOT_FOUND(305, "Transaction not found", HttpStatus.OK),
    BET_REJECTED(303, "Bet rejected", HttpStatus.OK),
    INVALID_PLAYER(304, "Invalid player", HttpStatus.OK);

    public final Integer code;
    public final String message;
    public final HttpStatus httpStatus;
}
