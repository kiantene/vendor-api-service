package com.nextgen.gameaggregator.vendor.bglive.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {

    SYSTEM_ERROR(000, "System Error");
//    NETWORK_ERROR(001, "network error"),
//    ERROR(002, "application layer unknown exception"),
//    ERROR(003, "data access unknown exception"),
//    ERROR(004, "cache access unknown exception"),
//    ERROR(005, "RPC call unknown exception"),
//    ERROR(100, "Incoming parameters are incorrect"),
//    ERROR(101, "JSON string syntax is incorrect"),
//    ERROR(501, "Server Interval Error!");
//
//    INVALID_DATA(400, "INVALID_DATA"),
//    INCORRECT_SESSION_TYPE(403, "INCORRECT_SESSION_TYPE"),
//    INVALID_SESSION(404, "INVALID_SESSION"),
//    ERROR(500, "ERROR");

//    INVALID_DATA(400, "INVALID_DATA", HttpStatus.SC_BAD_REQUEST),
//    INCORRECT_SESSION_TYPE(403, "INCORRECT_SESSION_TYPE", HttpStatus.SC_FORBIDDEN),
//    INVALID_SESSION(404, "INVALID_SESSION", HttpStatus.SC_NOT_FOUND),
//    INVALID_TRANSACTION(404, "INVALID_TRANSACTION", HttpStatus.SC_NOT_FOUND),
//    INSUFFICIENT_FUNDS(409, "INSUFFICIENT_FUNDS", HttpStatus.SC_CONFLICT),
//    INSUFFICIENT_CLEARED_FUNDS(404, "INSUFFICIENT_CLEARED_FUNDS", HttpStatus.SC_CONFLICT),
//    ERROR(500, "ERROR", HttpStatus.SC_INTERNAL_SERVER_ERROR);

    public final Integer code;
    public final String message;
}
