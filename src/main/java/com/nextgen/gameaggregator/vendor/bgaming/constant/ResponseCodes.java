package com.nextgen.gameaggregator.vendor.bgaming.constant;

import lombok.AllArgsConstructor;
import org.apache.http.HttpStatus;

@AllArgsConstructor
public enum ResponseCodes {

    SUCCESS(null, null, HttpStatus.SC_OK),
    REQUEST_SIGN_DOES_NOT_MATCH(HttpStatus.SC_FORBIDDEN, "Request sign doesn't match.", HttpStatus.SC_FORBIDDEN),
    FUND_NOT_ENOUGH(HttpStatus.SC_CONTINUE, "Funds not enough.", HttpStatus.SC_PRECONDITION_FAILED),
    CURRENCY_NOT_SUPPORT(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Currency not support (maybe need integrate new currency code for handle convert amount).", HttpStatus.SC_BAD_REQUEST),
    UNKNOWN_ERROR(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unknown error.", HttpStatus.SC_BAD_REQUEST),
    BET_ACTION_NOT_FOUND(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Action id not found.", HttpStatus.SC_BAD_REQUEST);

    public final Integer code;
    public final String message;
    public final Integer httpStatus;
}