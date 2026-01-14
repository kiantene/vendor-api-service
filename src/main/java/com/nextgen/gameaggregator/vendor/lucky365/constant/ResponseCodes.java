package com.nextgen.gameaggregator.vendor.lucky365.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseCodes {

    SUCCESS("S100", "Success", HttpStatus.OK, false),
    DUPLICATE_REQUEST("S200", "Repeated request", HttpStatus.OK, false),
    INVALID_SIGNATURE("F0001", "Invalid signature", HttpStatus.OK, false),
    PLAYER_EXISTING("F0005", "Player already exist", HttpStatus.OK, false),
    INVALID_PARAMETER("F0003", "Invalid parameter", HttpStatus.OK, false),
    PLAYER_NOT_FOUND("F0010", "Invalid player status", HttpStatus.OK, false),
    BET_NOT_FOUND("F0013", "No matching data", HttpStatus.OK, false),
    INSUFFICIENT_BALANCE("F0023", "Insufficient balance", HttpStatus.OK, false),
    INTERNAL_ERROR("M0002", "System error", HttpStatus.OK, false);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    private final boolean vendorWillRetry;
}