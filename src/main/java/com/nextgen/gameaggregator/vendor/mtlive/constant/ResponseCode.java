package com.nextgen.gameaggregator.vendor.mtlive.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseCode {

    SUCCESS               ("00000", "Success",                              HttpStatus.OK, false),
    DATA_ALREADY_EXISTS   ("00102", "Data already exists",                  HttpStatus.OK, false),
    INVALID_PARAMETER     ("20001", "Invalid parameter",                    HttpStatus.OK, false),
    DECRYPTION_ERROR      ("20002", "Decryption error",                     HttpStatus.OK, false),
    SYSTEM_MAINTENANCE    ("20003", "System maintenance",                   HttpStatus.OK, false),
    EXECUTION_FAILED      ("20004", "Failed",                               HttpStatus.OK, false),

    PLAYER_NOT_FOUND      ("20101", "The player's currency doesn't exist.", HttpStatus.OK, false),
    INSUFFICIENT_BALANCE  ("20102", "Balance is not enough",                HttpStatus.OK, false),

    ORDER_NOT_FOUND       ("20201", "The SequenNumber doesn't exist",       HttpStatus.OK, false),
    DUPLICATE_ORDER       ("20202", "Duplicate SequenNumber",               HttpStatus.OK, false),
    ORDER_SETTLED         ("20203", "This SequenNumber has been settled.",  HttpStatus.OK, false),
    ORDER_CANCELED        ("20204", "This SequenNumber has been cancelled.",HttpStatus.OK, false),

    DUPLICATE_TRANSACTION ("20501", "Duplicate TransactionId.",             HttpStatus.OK, false);

    public final String code;
    public final String message;
    public final HttpStatus httpStatus;
    private final boolean vendorWillRetry;
}