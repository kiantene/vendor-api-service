package com.nextgen.gameaggregator.custodianseamless.constant;

import lombok.AllArgsConstructor;


@AllArgsConstructor
public enum TransactionStatus {
    PROCESSING (1, "PROCESSING"),
    SUCCESS (2, "SUCCESS"),
    FAIL (3, "FAIL")
    ;
    public final Integer status;
    public final String description;

    public static String getDescriptionByStatus(Integer status) {
        for (TransactionStatus transactionStatus : values()) {
            if (transactionStatus.status.equals(status)) {
                return transactionStatus.description;
            }
        }
        // Handle the case where the status is not found
        return "Unknown Status";
    }
}