package com.nextgen.gameaggregator.vendor.facai.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {
    SUCCESS             (0, "Success"),
    PLAYER_NOT_FOUND   (500, "Account does not exist."),
    UNEXPECTED_ERROR     (999, "Unexpected error.")
    ;

    public final Integer code;
    public final String description;
}
