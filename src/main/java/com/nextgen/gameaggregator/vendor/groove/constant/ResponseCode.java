package com.nextgen.gameaggregator.vendor.groove.constant;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum ResponseCode {

    SUCCESS(200, "Success", HttpStatus.OK, false),
    DUPLICATE_SUCCESS(200, "Success - duplicate request", HttpStatus.OK, false),
    TECHNICAL_ERROR(1, "Technical error", HttpStatus.OK, false),
    WAGER_NOT_FOUND(102, "Wager not found", HttpStatus.OK, false),
    OPERATION_NOT_ALLOWED(110, "Operation not allowed", HttpStatus.OK, false),
    TXN_OPERATOR_MISMATCH(400, "Transaction operator mismatch", HttpStatus.OK, false),
    ROUND_CLOSED_OR_DUPLICATE_TXN(409, "Round closed or transaction ID exists", HttpStatus.OK, false),
    NOT_LOGGED_ON(1000, "Not logged on", HttpStatus.OK, false),
    INVALID_SIGNATURE(1001, "Invalid signature", HttpStatus.OK, false),
    AUTHENTICATION_FAILED(1003, "Authentication Failed", HttpStatus.OK, false),
    OUT_OF_MONEY(1006, "Out of money", HttpStatus.OK, false),
    UNKNOWN_CURRENCY(1007, "Unknown currency", HttpStatus.OK, false),
    PARAMETER_REQUIRED(1008, "Parameter required", HttpStatus.OK, false),
    GAMING_LIMIT(1019, "Loss limit exceeded / Overall bet limit exceeded", HttpStatus.OK, false),
    ACCOUNT_BLOCKED(1035, "Account blocked", HttpStatus.OK, false);

    public final Integer code;
    public final String message;
    public final HttpStatus httpStatus;
    public final boolean vendorWillRetry;
}
