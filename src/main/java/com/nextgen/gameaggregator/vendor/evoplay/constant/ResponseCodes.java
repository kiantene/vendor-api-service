package com.nextgen.gameaggregator.vendor.evoplay.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {
    SUCCESS("ok", "OK"),
    ERROR("error", "Error"),
    INVALID_REQUEST_ERROR("error", "Invalid Request"),
    PROCESSING_ERROR("error", "Processing Error"),
    IDEMPOTENT_ERROR("error", "Duplicate Request Error"),

    TEMPORARY_ERROR("error", "Temporary Error"),
    INSUFFICIENT_BALANCE_ERROR("error", "Insufficient Balance"),
    UNKNOWN_ERROR("error", "Unknown Error");

    public final String status;
    public final String message;

}
