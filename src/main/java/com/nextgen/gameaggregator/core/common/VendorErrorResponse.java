package com.nextgen.gameaggregator.core.common;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

@Getter
public class VendorErrorResponse {
    private final int statusCode;
    private final Object body;

    public VendorErrorResponse(Object body) {
        this(HttpServletResponse.SC_UNAUTHORIZED, body); // default to 401
    }

    public VendorErrorResponse(int statusCode, Object body) {
        this.statusCode = statusCode;
        this.body = body;
    }
}
