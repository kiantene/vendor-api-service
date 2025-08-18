package com.nextgen.gameaggregator.core.exception.mapper;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class VendorErrorResponse {
    private final HttpStatus statusCode;
    private final Object body;

    public VendorErrorResponse(Object body) {
        this(HttpStatus.OK, body); // default to 200, unless override
    }

    public VendorErrorResponse(HttpStatus statusCode, Object body) {
        this.statusCode = statusCode;
        this.body = body;
    }
}
