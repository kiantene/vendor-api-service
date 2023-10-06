package com.nextgen.gameaggregator.vendor.habanero.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {
    AUTH_SUCCESS(true, false, null, null, null, ""),
    QUERY_SUCCESS(true, null, null, null, null, null),
    QUERY_FALSE(false, null, null, null, null, null),
    TRANSFER_SUCCESS(true, null, null, null, null, null),
    REFUNDED(null, null, null, null, 1, null),
    REFUND_NOT_REQUIRE(null, null, null, null, 2, null),
    AUTHENTICATE_ERROR(false, true, null, null, null, "Authentication failed."),
    TRANSFER_ERROR(false, true, null, null, null, "Invalid transfer."),
    INSUFFICIENT_ERROR(false, null, true, null, null, "Invalid transfer."),
    RETRY_ERROR(false, null, null, true, null, null);


    public final Boolean success;
    public final Boolean authError;
    public final Boolean noFunds;
    public final Boolean retryStatus;
    public final Integer refundStatus;

    public final String message;

}