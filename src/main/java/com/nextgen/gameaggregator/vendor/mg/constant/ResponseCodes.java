package com.nextgen.gameaggregator.vendor.mg.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {
    SUCCESS                 (200, "Ok"),
    BAD_REQUEST             (400, "Bad Request"),
    UNAUTHORIZED            (401, "Unauthorized"),
    BALANCE_NOT_ENOUGH      (402, "Not enough available balance"),
    FORBIDDEN               (403, "Forbidden"),
    NOT_FOUND               (404, "Not Found"),
    INTERNAL_SERVER_ERROR   (500, "Internal Server Error")
    ;

    public final Integer code;
    public final String description;
}
