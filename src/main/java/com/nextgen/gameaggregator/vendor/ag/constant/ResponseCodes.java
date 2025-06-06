package com.nextgen.gameaggregator.vendor.ag.constant;

import lombok.AllArgsConstructor;
import org.apache.http.HttpStatus;

@AllArgsConstructor

public enum ResponseCodes {
    OK(200,"OK", HttpStatus.SC_OK),
    INVALID_DATA(400,"INVALID_DATA", HttpStatus.SC_BAD_REQUEST),
    INCORRECT_SESSION_TYPE(403,"INCORRECT_SESSION_TYPE", HttpStatus.SC_FORBIDDEN),
    INVALID_SESSION(404,"INVALID_SESSION", HttpStatus.SC_NOT_FOUND),
    ERROR(500,"ERROR", HttpStatus.SC_INTERNAL_SERVER_ERROR);



    public final Integer code;
    public final String message;
    public final Integer httpStatus;

}
