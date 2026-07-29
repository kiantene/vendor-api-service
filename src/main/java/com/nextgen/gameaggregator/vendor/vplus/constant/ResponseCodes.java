package com.nextgen.gameaggregator.vendor.vplus.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

//@formatter:off
@Getter
@AllArgsConstructor
public enum ResponseCodes {

    SUCCESS                     (0, "Success", HttpStatus.OK, false),
    USER_NOT_FOUND              (1, "User not found", HttpStatus.OK, false),
    SYSTEM_ERROR                (2, "System error", HttpStatus.OK, false),
    BETTING_NOT_ALLOWED         (3, "Betting not allowed", HttpStatus.OK, false),
    VERIFICATION_FAILED         (4, "Verification failed", HttpStatus.OK, false),
    PLAYER_FROZEN               (5, "Player is frozen", HttpStatus.OK, false),
    INVALID_REQUEST_PARAMETERS  (6, "Invalid request parameters", HttpStatus.OK, false),
    GAME_NOT_FOUND_OR_DISABLED  (7, "Game not found or disabled", HttpStatus.OK, false),
    BETTING_LIMIT_REACHED       (8, "Betting limit reached", HttpStatus.OK, false),
    DUPLICATE_REQUEST           (9, "Duplicate request", HttpStatus.OK, false),
    BET_ALREADY_CANCELED        (10, "Bet already canceled", HttpStatus.OK, false),
    BET_CONFIRMED_NOT_CANCELABLE(11, "Bet confirmed and cannot be canceled", HttpStatus.OK, false),
    OTHER_ERROR                 (12, "Other error", HttpStatus.OK, false);

    private final Integer code;
    private final String message;
    private final HttpStatus httpStatus;
    private final boolean vendorWillRetry;
}
