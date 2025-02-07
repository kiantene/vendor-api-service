package com.nextgen.gameaggregator.vendor.bglive.constant;

import lombok.AllArgsConstructor;
import org.apache.http.HttpStatus;

@AllArgsConstructor
public enum ResponseCodes {

    ERROR(501, "Server Interval Error!", HttpStatus.SC_INTERNAL_SERVER_ERROR);
//    INVALID_DATA(400, "INVALID_DATA", HttpStatus.SC_BAD_REQUEST),
//    INCORRECT_SESSION_TYPE(403, "INCORRECT_SESSION_TYPE", HttpStatus.SC_FORBIDDEN),
//    INVALID_SESSION(404, "INVALID_SESSION", HttpStatus.SC_NOT_FOUND),
//    INVALID_TRANSACTION(404, "INVALID_TRANSACTION", HttpStatus.SC_NOT_FOUND),
//    INSUFFICIENT_FUNDS(409, "INSUFFICIENT_FUNDS", HttpStatus.SC_CONFLICT),
//    INSUFFICIENT_CLEARED_FUNDS(404, "INSUFFICIENT_CLEARED_FUNDS", HttpStatus.SC_CONFLICT),
//    ERROR(500, "ERROR", HttpStatus.SC_INTERNAL_SERVER_ERROR);

    public final Integer code;
    public final String message;
    public final Integer httpStatus;
}
