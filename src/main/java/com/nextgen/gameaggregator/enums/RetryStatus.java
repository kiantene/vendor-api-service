package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum RetryStatus {
    FAILED(0, "Failed within 6 times"),
    SUCCESS(1, "Active"),
    TIMEOUT(2, "Failed more than 7 times");

    public final Integer code;
    public final String description;
}
