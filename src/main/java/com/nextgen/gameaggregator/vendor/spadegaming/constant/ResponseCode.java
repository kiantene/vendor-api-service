package com.nextgen.gameaggregator.vendor.spadegaming.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    SUCCESS                 (0, "Success"),
    SYSTEM_ERROR            (1, "System Error"),
    INVALID_REQUEST         (2, "Invalid Request."),
    SERVICE_INACCESSIBLE    (3, "Service Inaccessible"),
    REQUEST_TIMEOUT         (100, "Request Timeout"),
    CALL_LIMITED            (101, "Call Limited"),
    MISSING_PARAMETER       (105, "Missing Parameter"),
    INVALID_PARAMETER       (106, "Invalid Parameters"),
    DUPLICATED_SERIAL_NO    (107, "Duplicated Serial NO"),
    RELATED_ID_NOT_FOUND    (109, "Related id not found"),
    RECORD_ID_NOT_FOUND     (110, "Record ID Not Found"),
    DUPLICATED_REQUEST      (111, "Duplicated request"),
    INVALID_FORMAT          (118, "Invalid Format"),
    MERCHANT_NOT_FOUND      (10113, "Merchant Not Found"),
    ACCT_NOT_FOUND          (50100, "Acct Not Found"),
    TOKEN_VALIDATION_FAILED (50104, "Token Validation Failed"),
    INSUFFICIENT_BALANCE    (50110, "Insufficient Balance"),
    CURRENCY_INVALID        (50112, "Currency Invalid")
    ;

    public final Integer code;
    public final String description;
}
