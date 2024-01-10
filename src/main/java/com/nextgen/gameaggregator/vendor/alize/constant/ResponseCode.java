package com.nextgen.gameaggregator.vendor.alize.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    SUCCESS            (0, "SC_OK"),
    ERROR              (-1, "SC_ERROR"),
    REFUSED_CANCEL     (-2, "Refuse to cancel this transaction.");

    public final Integer code;
    public final String description;
}
