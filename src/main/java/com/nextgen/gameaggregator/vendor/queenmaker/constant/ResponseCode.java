package com.nextgen.gameaggregator.vendor.queenmaker.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    INVALID_OR_EXPIRED_TOKEN(10, "Invalid or expired token"),
    INVALID_TOKEN_SCOPE(19, "Invalid token scope"),
    USER_BLOCKED(20, "User blocked"),
    INVALID_CREDENTIAL(30, "Invalid Credential"),
    CURRENCY_MISMATCH(50, "Currency Mismatch"),
    INSUFFICIENT_FUNDS(100, "Insufficient funds to perform the operation"),
    INVALID_ARGUMENTS(300, "Invalid arguments: {replace}"),
    MISSING_TOKEN(400, "Missing token"),
    INCORRECT_FORMAT(500, "Incorrect format. {replace}"),
    TRANSACTION_DOES_NOT_EXIST(600, "Transaction does not exist"),
    TRANSACTION_ALREADY_CANCELLED(610, "Transaction already cancelled"),
    OPERATION_FAILED_DETERMINISTICALLY(800, "Operation Failed Deterministically"),
    SYSTEM_ERROR(900, "System Error. {replace}"),
    CONFIGURED_TIMEOUT_EXCEEDED(903, "Configured Timeout Exceeded");
    public final Integer err;
    public final String errdesc;
}

