package com.nextgen.gameaggregator.vendor.dreamgaming.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    SUCCESS(0, "Success"),
    PARAMETER_ERROR(1, "Parameter Error"),
    TOKEN_VERIFICATION_FAILED(2, "Token Verification Failed"),
    COMMAND_NOT_FOUND(3, "Command Not Find"),
    ILLEGAL_OPERATION(4, "Illegal Operation"),
    DATE_FORMAT_ERROR(10, "Date format error"),
    DATA_FORMAT_ERROR(11, "Data format error"),
    PERMISSION_DENIED(97, "Permission denied"),
    OPERATION_FAILED(98, "Operation failed"),
    UNKNOWN_ERROR(99, "Unknown Error"),
    ACCOUNT_LOCKED(100, "Account is locked"),
    ACCOUNT_FORMAT_ERROR(101, "Account format error"),
    ACCOUNT_NOT_EXIST(102, "Account does not exist"),
    ACCOUNT_TAKEN(103, "This account is taken"),
    PASSWORD_FORMAT_ERROR(104, "Password format error"),
    PASSWORD_WRONG(105, "Password wrong"),
    SAME_PASSWORD(106, "New & Old Password is the same"),
    MEMBER_ACCOUNT_UNAVAILABLE(107, "Member account unavailable"),
    LOGIN_ERROR(108, "Login Error"),
    SIGNUP_ERROR(109, "Signup Error"),
    ACCOUNT_SIGNED_IN(110, "This account has been signed in"),
    ACCOUNT_SIGNED_OUT(111, "This account has been signed out"),
    ACCOUNT_NOT_SIGNED_IN(112, "This account is not signed in"),
    AGENT_ACCOUNT_INVALID(113, "The Agent account inputted is not an Agent account"),
    MEMBER_NOT_FOUND(114, "Member not found"),
    ACCOUNT_OCCUPIED(116, "Account occupied"),
    BRANCH_NOT_FOUND(117, "Can not find branch of member"),
    AGENT_NOT_FOUND(118, "Can not find the specified Agent"),
    INSUFFICIENT_FUNDS_AGENT_WITHDRAWAL(119, "Insufficent funds during Agent withdrawal"),
    INSUFFICIENT_BALANCE(120, "Insufficient balance"),
    PROFIT_LIMIT_ERROR(121, "Profit limit must be greater than or equal to 0"),
    FREE_DEMO_ACCOUNT_EXHAUSTED(150, "Ran out of free demo accounts"),
    //300
    SYSTEM_MAINTENANCE(300, "System maintenance"),
    WRONG_API_KEY(320, "Wrong API key"),
    LIMIT_GROUP_NOT_FOUND(321, "Limit Group Not Found"),
    CURRENCY_NAME_NOT_FOUND(322, "Currency Name Not Found"),
    USE_SERIAL_NUMBERS_FOR_TRANSFER(323, "Use serial numbers for Transfer"),
    TRANSFER_FAILED(324, "Transfer failed"),
    AGENT_STATUS_UNAVAILABLE(325, "Agent Status Unavailable"),
    MEMBERS_AGENT_NO_VIDEO_GROUP(326, "Members Agent No video group"),
    //400
    CLIENT_IP_RESTRICTED(400, "Client IP Restricted"),
    NETWORK_LATENCY(401, "Network latency"),
    CONNECTION_CLOSED(402, "The connection is closed"),
    CLIENTS_LIMITED_SOURCES(403, "Clients limited sources"),
    RESOURCE_NOT_FOUND(404, "Resource requested does not exist"),
    TOO_FREQUENT_REQUESTS(405, "Too frequent requests"),
    REQUEST_TIMED_OUT(406, "Request timed out"),
    GAME_ADDRESS_NOT_FOUND(407, "Can not find game address"),
    //500
    NULL_POINTER_EXCEPTION(500, "Null pointer exception"),
    SYSTEM_ERROR(501, "System Error"),
    SYSTEM_BUSY(502, "The system is busy"),
    DATA_OPERATION_ERROR(503, "Data operation error");

    public final Integer code;
    public final String message;

    public static ResponseCode fromCode(int code) {
        for (ResponseCode responseCode : ResponseCode.values()) {
            if (responseCode.code == code) {
                return responseCode;
            }
        }
        throw new IllegalArgumentException("Unknown response code: " + code);
    }
}
