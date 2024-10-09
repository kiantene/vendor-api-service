package com.nextgen.gameaggregator.operator.constant;

import lombok.AllArgsConstructor;

public class ResponseCodes {
    @AllArgsConstructor
    public enum Status {

        SC_TRANSACTION_STILL_PROCESSING(0, "Transaction is still processing, please retry."),
        SC_OK(1, "Successful response."),
        SC_UNKNOWN_ERROR(2, "Generic status code for unknown errors."),
        SC_INVALID_REQUEST(3, "Wrong/missing parameters sent in request body."),
        SC_AUTHENTICATION_FAILED(4, "Authentication failed. X-API-Key is missing or invalid."),
        SC_INVALID_SIGNATURE(5, "X-Signature verification failed."),
        SC_INVALID_TOKEN(6, "Invalid token on Operator's system."),
        SC_INVALID_GAME(7, "Not a valid game."),
        SC_DUPLICATE_REQUEST(8, "Duplicate request."),
        SC_CURRENCY_NOT_SUPPORTED(9, "Currency is not supported."),
        SC_WRONG_CURRENCY(10, "Transaction's currency is different from user's wallet currency."),
        SC_INSUFFICIENT_FUNDS(11, "User's wallet does not have enough funds."),
        SC_USER_NOT_EXISTS(12, "User does not exists in Operator's system"),
        SC_USER_DISABLED(13, "User is disabled and not allowed to place bets."),
        SC_TRANSACTION_DUPLICATED(14, "Duplicate transaction Id was sent."),
        SC_TRANSACTION_NOT_EXISTS(15, "Corresponding bet transaction cannot be found."),
        SC_VENDOR_ERROR(16, "Error encountered on game vendor."),
        //SC_INVALID_CURRENCY (17, "Currency is not supported"),
        SC_UNDER_MAINTENANCE(18, "Game is under maintenance."),
        SC_MISMATCHED_DATA_TYPE(19, "Invalid data type."),
        SC_INVALID_RESPONSE(20, "Invalid response"),
        SC_INVALID_VENDOR(22, "Vendor is not supported"),
        SC_INVALID_LANGUAGE(23, "Language is not supported"),
        SC_GAME_DISABLED(24, "Game is disabled"),
        SC_INVALID_PLATFORM(23, "Platform is not supported"),
        SC_GAME_LANGUAGE_NOT_SUPPORTED(24, "Game language is not supported"),
        SC_GAME_PLATFORM_NOT_SUPPORTED(25, "Game platform is not supported"),
        SC_GAME_CURRENCY_NOT_SUPPORTED(26, "Game currency is not supported"),
        SC_VENDOR_LINE_DISABLED(27, "Vendor line is disable"),
        SC_VENDOR_CURRENCY_NOT_SUPPORTED(28, "Vendor currency is not supported"),
        SC_VENDOR_LANGUAGE_NOT_SUPPORTED(29, "Vendor language is not supported"),
        SC_VENDOR_PLATFORM_NOT_SUPPORTED(30, "Vendor platform is not supported"),
        SC_EXCEEDED_NUMBER_OF_RETRIES(31, "Exceeded number of retries."),
        SC_OPERATOR_TIMEOUT(32, "Operator timed out"),
        SC_INVALID_FROM_TIME(33, "Data only available last 60 days"),
        SC_INVALID_DATE_RANGE(34, "Date range should be within one day."),

        SC_REFERENCE_ID_DUPLICATED(35, "Duplicate reference Id was sent."),
        SC_TRANSACTION_DOES_NOT_EXIST(36, "Corresponding reference Id cannot be found."),
        SC_INTERNAL_ERROR(37, "Internal error. please checked in relevant support channel"),

        SC_WALLET_NOT_SUPPORTED(38, "Wallet Type is not supported."),
        SC_INVALID_GAME_TOPIC(39, "Invalid game topic subscribed; Supported: 1.) hot 2.) top."),
        SC_TRACE_ID_MISMATCHED(40, "Trace Id mismatched"),
        SC_USERNAME_MISMATCHED(41, "Request username is different from Response username"),
        SC_INVALID_DATE_7_RANGE(42, "Date range should be within seven days."),

        ;


        public final Integer code;
        public final String description;

        public static Status checkCodeStatus(Integer code) {
            Status status = SC_UNKNOWN_ERROR;
            for (Status checkStatus : Status.values()) {
                if (checkStatus.code.equals(code)) {
                    status = checkStatus;
                }
            }
            return status;
        }

    }

}
