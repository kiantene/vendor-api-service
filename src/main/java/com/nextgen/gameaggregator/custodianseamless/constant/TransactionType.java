package com.nextgen.gameaggregator.custodianseamless.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TransactionType {
    DEPOSIT (1, "DEPOSIT"),
    WITHDRAWAL (2, "WITHDRAWAL")
    ;

    public final Integer status;
    public final String description;

    public static String getTransactionTypeByStatus(Integer status) {
        for (TransactionType transactionType : values()) {
            if (transactionType.status.equals(status)) {
                return transactionType.description;
            }
        }
        // Handle the case where the status is not found
        return "Unknown Status";
    }
}