package com.nextgen.gameaggregator.vendor.egtdigital.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseCodes {

    OK                              ("OK", "Request successful", HttpStatus.OK, false),
    ERR_INTEGRITY_CHECK_FAILED      ("ERR_INTEGRITY_CHECK_FAILED", "Message integrity check failed", HttpStatus.OK, false),
    ERR_INVALID_ACCOUNT             ("ERR_INVALID_ACCOUNT", "In case the player currency does not match the provided currency code in the request.", HttpStatus.OK, false),
    ERR_INVALID_TOKEN               ("ERR_INVALID_TOKEN", "The supplied defence code token is not valid", HttpStatus.OK, false),
    ERR_INVALID_PLAYER_ID           ("ERR_INVALID_PLAYER_ID", "Invalid player ID", HttpStatus.OK, false),
    ERR_UNKNOWN                     ("ERR_UNKNOWN", "Internal server error", HttpStatus.OK, false),
    ERR_TIMEOUT                     ("ERR_TIMEOUT", "Timeout exception", HttpStatus.OK, false),
    ERR_NOT_ENOUGH_MONEY            ("ERR_NOT_ENOUGH_MONEY", "Player account does not have sufficient funds to complete the operation.", HttpStatus.OK, false),
    ERR_LIMIT_REACHED                     ("ERR_LIMIT_REACHED", "Occurs when the player has exceeded his game limits (responsible gaming).", HttpStatus.OK, false),
    ERR_TRANSFER_DOES_NOT_EXIST     ("ERR_TRANSFER_DOES_NOT_EXIST", "The referenced transfer is already reversed. Receiving this error will stop the scheduled retry job for this particular transaction.", HttpStatus.OK, false),
    ERR_TRANSFER_ROLLED_BACK        ("ERR_TRANSFER_ROLLED_BACK", "The referenced transfer is already reversed. Receiving this error will stop the scheduled retry job for this particular transaction.", HttpStatus.OK, false),
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    private final boolean vendorWillRetry;
}
