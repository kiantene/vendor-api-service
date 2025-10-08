package com.nextgen.gameaggregator.vendor.aviatorstudio.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseCodes {

    SUCCESS             (200, "Success", HttpStatus.OK, false),
    INSUFFICIENT_FUNDS  (400, "Insufficient funds", HttpStatus.BAD_REQUEST, false),
    AUTH_ERROR          (403, "Authentication failed", HttpStatus.FORBIDDEN, false),
    SERVER_ERROR        (500, "Server error", HttpStatus.INTERNAL_SERVER_ERROR, false);

    private final Integer code;
    private final String description;
    private final HttpStatus httpStatus;
    private final boolean vendorWillRetry;
}
