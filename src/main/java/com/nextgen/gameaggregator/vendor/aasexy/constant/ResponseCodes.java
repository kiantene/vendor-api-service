package com.nextgen.gameaggregator.vendor.aasexy.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {
    SUCCESS("0000", "Success"),
    INVALID_USER_ID("1000", "Invalid user Id"),
    INVALID_CURRENCY("1004", "Invalid Currency"),
    INVALID_TOKEN("1008", "Invalid token"),
    INSUFFICIENT_BALANCE("1018", "Not Enough Balance"),
    ACCOUNT_LOCKED("1013", "Account is Lock"),
    INVALID_GAME("1033", "Invalid Game"),
    INVALID_PARAMETERS("1036", "Invalid parameters"),
    DUPLICATE_TRANSACTION("1038", "Duplicate transaction"),
    TRANSACTION_NOT_FOUND("1039", "Transaction not found"),
    SYSTEM_BUSY("9998", "System Busy"),
    FAIL("9999", "Fail");

    public final String status;
    public final String desc;

}
