package com.nextgen.gameaggregator.vendor.gpkv2.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseCodes {
    SUCCESS               (0, "Success", HttpStatus.OK, false),
    PLAYER_NOT_FOUND      (2006, "Player not found or invalid session token", HttpStatus.OK, true),
    INSUFFICIENT_BALANCE  (2007, "Player balance not enough", HttpStatus.OK, true);

    private final Integer code;
    private final String message;
    private final HttpStatus httpStatus;
    private final boolean vendorWillRetry;
}
