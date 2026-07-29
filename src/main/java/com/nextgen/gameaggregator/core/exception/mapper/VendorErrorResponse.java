package com.nextgen.gameaggregator.core.exception.mapper;

import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Map;

@Getter
public class VendorErrorResponse {
    private final HttpStatus statusCode;
    private final Object body;
    private final HttpHeaders headers;

    public VendorErrorResponse(Object body) {
        this(HttpStatus.OK, body, null); // default to 200, unless override
    }

    public VendorErrorResponse(HttpStatus statusCode, Object body) {
        this(statusCode, body, null);
    }

    public VendorErrorResponse(HttpStatus statusCode, Object body, Map<String, ?> headers) {
        this.statusCode = statusCode;
        this.body = body;

        if (headers == null || headers.isEmpty()) {
            this.headers = null;
            return;
        }

        this.headers = new HttpHeaders();

        for (String key : headers.keySet()) {
            if (key == null || headers.get(key) == null) {
                continue;
            }
            this.headers.add(key, headers.get(key).toString());
        }

        if (this.headers.getContentType() == null) {
            this.headers.setContentType(MediaType.APPLICATION_JSON);
        }
    }


    public boolean hasHeaders() {
        return headers != null && !headers.isEmpty();
    }
}
