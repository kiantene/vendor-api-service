package com.nextgen.gameaggregator.vendor.jdb.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    SUCCESS("0000", "Succeed"),
    INSUFFICIENT_BALANCE("6006", "Your Cash Balance not enough."),
    PLAYER_NOT_FOUND("7501", "User ID cannot be found."),
    INVALID_ACTION("9007", "Unknown action."),
    INVALID_REQUEST_PARAMETER("8000", "The parameter of input error, please check your parameter is correct or not."),
    FAILED("9999", "Failed");

    public final String code;
    public final String description;
}
