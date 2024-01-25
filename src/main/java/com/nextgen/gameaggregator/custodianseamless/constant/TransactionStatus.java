package com.nextgen.gameaggregator.custodianseamless.constant;

import lombok.AllArgsConstructor;


@AllArgsConstructor
public enum TransactionStatus {
    PROCESSING (1, "processing"),
    SUCCESS (3, "success"),
    FAIL (2, "fail")
    ;
    public final Integer status;
    public final String description;
}