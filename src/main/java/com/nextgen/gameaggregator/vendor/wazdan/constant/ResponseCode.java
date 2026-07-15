package com.nextgen.gameaggregator.vendor.wazdan.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseCode {

    SUCCESS(0, "Success", HttpStatus.OK, false),

    SESSION_NOT_FOUND(1, "Session not found", HttpStatus.OK, false),
    SESSION_EXPIRED(2, "Session expired", HttpStatus.OK, false),
    SESSION_ALREADY_EXISTS(3, "Session already exists", HttpStatus.OK, false),
    LIMIT_REACHED(4, "Limit reached", HttpStatus.OK, false),
    USER_BLOCKED(5, "User is blocked", HttpStatus.OK, false),
    INSUFFICIENT_FUNDS(8, "Insufficient funds", HttpStatus.OK, false),
    IP_NOT_ALLOWED(9, "Player's IP is not allowed", HttpStatus.OK, false),

    // Generic / fallback codes
    SYSTEM_ERROR(1000, "Technical problem", HttpStatus.OK, false),
    DUPLICATE_REQUEST(1001, "Duplicate request", HttpStatus.OK, false);


    public final Integer code;
    public final String message;
    public final HttpStatus httpStatus;
    public final boolean vendorWillRetry;
}
