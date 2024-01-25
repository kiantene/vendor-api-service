package com.nextgen.gameaggregator.custodianseamless.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TransactionType {
    DEPOSIT (1, "deposit amount"),
    WITHDRAWAL (3, "withdraw  amount")
    ;

    public final Integer status;
    public final String description;
}