package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Status {
    INACTIVE (0, "Inactive"),
    ACTIVE (1, "Active")
    ;

    public final Integer code;
    public final String description;
}
