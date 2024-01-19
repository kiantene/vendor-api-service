package com.nextgen.gameaggregator.vendor.advantplay.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {
    SUCCESS(0, "Success"),
    UNSPECIFIED_ERROR(5000, "Unspecified Error"),
    GAME_NOT_FOUND(5011, "Game Not Found"),
    CURRENCY_NOT_SUPPORT(5012, "Currency Not Support"),
    UNDER_MAINTENANCE(5050, "Under Maintenance"),
    DB_ERROR(5060, "DB Error"),
    API_ERROR(5100, "API Error"),
    HASH_INVALID(5101, "Hash Invalid"),
    MANDATORY_PARAM_MISSING(5102, "Mandatory Param Missing"),
    PARAMETER_INCORRECT(5103, "Parameter Incorrect"),
    LOGIN_FAILED(5110, "Login Failed"),
    AUTHENTICATION_INCORRECT(5111, "Authentication Incorrect"),
    TOKEN_INVALID(5112, "Token Invalid"),
    TOKEN_EXPIRED(5113, "Token Expired"),
    DATA_INVALID(5114, "Data Invalid"),
    ACCESS_ERROR(5115, "Access Error"),
    DUPLICATE_REQUEST(5121, "Duplicate Request"),
    IP_RESTRICTED(5201, "IP Restricted"),
    ACCOUNT_ALREADY_EXIST(5211, "Account Already Exist"),
    ACCOUNT_NOT_EXIST(5212, "Account Not Exist"),
    ACCOUNT_LOCKED(5213, "Account Locked"),
    ACCOUNT_IS_BLACKLISTED(5214, "Account Is Blacklisted"),
    PLAYER_NO_PERMITTED(5311, "Player No Permitted"),
    STAKE_ILLEGAL(5312, "Stake Illegal"),
    PLAYER_HAS_INSUFFICIENT_FUNDS(5321, "Player Has Insufficient Funds"),
    PRIZE_EXCEED_LIMIT(5322, "Prize Exceed Limit");

    public final Integer errorCode;
    public final String errorDescription;
}
