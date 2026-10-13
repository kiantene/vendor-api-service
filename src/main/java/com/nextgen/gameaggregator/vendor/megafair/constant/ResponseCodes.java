package com.nextgen.gameaggregator.vendor.megafair.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

//@formatter:off
@Getter
@AllArgsConstructor
public enum ResponseCodes {

    // Success (Standard)
    SUCCESS                         (0, "Success", HttpStatus.OK, false),

    // 6.1 Server-side errors
    DATABASE_ERROR                  (29, "Database error", HttpStatus.OK, false),
    ERROR_READING_TABLE             (30, "Error reading table", HttpStatus.OK, false),
    INVALID_TABLE_ID                (31, "Invalid Table ID", HttpStatus.OK, false),
    OPERATOR_SERVER_ERROR           (56, "Operator server error", HttpStatus.OK, true),

    // 6.2 Client-side errors
    CONNECTION_LOST                 (101, "Connection Lost", HttpStatus.OK, false),
    SYSTEM_ERROR_UNKNOWN            (114, "System error (An unknown system error occurred)", HttpStatus.OK, false),
    BROWSER_DEVICE_NOT_SUPPORTED    (120, "Browser or device is not supported", HttpStatus.OK, false),

    // 6.3 ES error codes
    INSUFFICIENT_FUNDS              (300, "Insufficient funds", HttpStatus.OK, false),
    OPERATION_FAILED                (301, "Operation failed", HttpStatus.OK, false),
    UNKNOWN_TRANSACTION_ID          (302, "Unknown transaction ID", HttpStatus.OK, false),
    UNKNOWN_USER_ID                 (309, "Unknown UserID", HttpStatus.OK, false),
    USER_NOT_FOUND                  (310, "User not found", HttpStatus.OK, false),
    INTERNAL_ERROR_ES               (399, "Internal Error", HttpStatus.OK, false),
    INVALID_TOKEN                   (400, "Invalid Token", HttpStatus.OK, false),

    // 6.4 Access restrictions
    ACCESS_DENIED                   (403, "Access denied", HttpStatus.OK, false),

    // 6.5 Hash generation errors
    INVALID_HASH                    (500, "Invalid Hash", HttpStatus.OK, false),

    // 6.6 Request & Session validation errors
    REQUEST_SYNTAX_MISMATCH         (609, "Received request doesn't match expected request form and syntax", HttpStatus.OK, false),
    INVALID_PARAMETERS              (610, "Invalid parameters", HttpStatus.OK, false),
    SESSION_NOT_FOUND               (612, "Session not found", HttpStatus.OK, false),

    // 6.7 Bingo Game error codes
    FAILED_TO_LAUNCH_GAME           (1001, "Failed to launch the game", HttpStatus.OK, false),
    FAILED_TO_ENTER_ROOM            (1002, "Failed to enter the room", HttpStatus.OK, false),
    FAILED_TO_RETRIEVE_RESULTS      (1003, "Failed to retrieve the tournament results", HttpStatus.OK, false),
    ERROR_LOADING_CONFIG            (1004, "Error while loading the game configuration", HttpStatus.OK, false),
    UNEXPECTED_ERROR_BINGO          (1005, "Unexpected error?", HttpStatus.OK, false),
    DOES_NOT_MATCH_PATTERN          (1006, "Does not match the pattern", HttpStatus.OK, false),
    JWT_LIFETIME_SHORT              (1007, "JWT session token lifetime is less than Room Lifetime", HttpStatus.OK, false),
    WEB_SESSION_LIFETIME_SHORT      (1008, "The web session lifetime is less than Room lifetime", HttpStatus.OK, false);

    private final Integer code;
    private final String message;
    private final HttpStatus httpStatus;
    private final boolean vendorWillRetry;
}
