package com.nextgen.gameaggregator.vendor.topbet.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseCode {

    SUCCESS(0, "Success", HttpStatus.OK, false),
    DUPLICATE_REQUEST(0, "Duplicate Request", HttpStatus.OK, false),
    SYSTEM_ERROR(1, "System error", HttpStatus.OK, false),
    API_PARAMETER_ERROR(2, "API parameter error", HttpStatus.OK, false),
    SIGNATURE_VERIFICATION_FAILED(3, "Signature verification failed", HttpStatus.OK, false),
    USER_NOT_FOUND(4, "User does not exist", HttpStatus.OK, false),
    INSUFFICIENT_BALANCE(5, "Insufficient user balance", HttpStatus.OK, false),
    ORDER_ALREADY_ROLLED_BACK(6, "The order has been rollback", HttpStatus.OK, false);

    public final Integer code;
    public final String message;
    public final HttpStatus httpStatus;
    public final boolean vendorWillRetry;
}
